package crocalert.app.shared.data.repository

import crocalert.app.domain.AlertStatusValidator
import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
import crocalert.app.model.AlertStatus
import crocalert.app.shared.data.local.AlertLocalDataSource
import crocalert.app.shared.data.mapper.toDto
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.AlertRemoteDataSource
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.sync.SyncPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

class AlertRepositoryImpl(
    private val remote: AlertRemoteDataSource,
    private val local: AlertLocalDataSource,
    private val syncPrefs: Flow<SyncPreferences> = kotlinx.coroutines.flow.flowOf(SyncPreferences()),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : AlertRepository {

    private val _lastRefreshError = MutableStateFlow<String?>(null)
    override val lastRefreshError: StateFlow<String?> = _lastRefreshError.asStateFlow()

    // Prevents concurrent syncIfStale() calls (one per subscriber) from firing parallel network requests.
    private val syncMutex = Mutex()

    override fun observeAlerts(): Flow<List<Alert>> =
        local.selectAll()
            .map { dtos -> dtos.map { it.toModel() } }
            .also { coroutineScope.launch { syncIfStale() } }

    override fun observeAlert(alertId: String): Flow<Alert?> =
        local.selectAll()
            .map { dtos -> dtos.firstOrNull { it.id == alertId }?.toModel() }
            .also { coroutineScope.launch { syncIfStale() } }

    override suspend fun createAlert(alert: Alert): String {
        val id = remote.createAlert(alert.toDto()).getOrThrow().id
        sync()
        return id
    }

    override suspend fun updateAlert(alert: Alert) {
        val id = alert.id.ifBlank { throw IllegalArgumentException("updateAlert necesita id") }
        // R-01: validate status transition against current cached state before hitting the network.
        val currentDto = local.selectAll().first().firstOrNull { it.id == id }
        if (currentDto != null) {
            val currentStatus = runCatching { AlertStatus.valueOf(currentDto.status) }
                .getOrElse { AlertStatus.OPEN }
            if (currentStatus != alert.status) {
                AlertStatusValidator.requireValidTransition(currentStatus, alert.status)
            }
        }
        remote.updateAlert(id, alert.toDto()).getOrThrow()
        sync()
    }

    override suspend fun deleteAlert(alertId: String) {
        remote.deleteAlert(alertId).getOrThrow()
        sync()
    }

    /** On-demand full refresh — ignores TTL and fetches all alerts. */
    override suspend fun refresh() = sync(since = null)

    /** Called by WorkManager sync workers. */
    internal suspend fun forceSync() = sync(since = null)

    private suspend fun syncIfStale() {
        try {
            syncMutex.withLock {
                val ttl = syncPrefs.first().alertsTtlMinutes
                val staleThreshold = Clock.System.now().minus(ttl.minutes).toEpochMilliseconds()
                val lastSync = local.lastSyncedAt() ?: 0L
                if (lastSync < staleThreshold) {
                    // Incremental: only fetch alerts newer than what we have cached.
                    // Falls back to full fetch (null) when the local cache is empty.
                    val since = local.latestCreatedAt()
                    sync(since = since)
                }
            }
        } catch (e: Exception) {
            _lastRefreshError.value = e.message ?: "Sync check failed"
        }
    }

    /**
     * Fetches alerts from the server.
     * [since] = epoch-ms of the newest alert in local cache; null → full fetch.
     */
    private suspend fun sync(since: Long? = null) {
        when (val res = remote.getAlerts(since = since)) {
            is ApiResult.Success -> {
                // Full sync (since=null) replaces entire cache so server-side deletions propagate.
                // Incremental sync (since≠null) merges to preserve older cached records.
                if (since == null) local.clearAndUpsertAll(res.data) else local.upsertAll(res.data)
                _lastRefreshError.value = null
            }
            is ApiResult.Error -> _lastRefreshError.value = res.message
        }
    }

    private fun <T> ApiResult<T>.getOrThrow(): T = when (this) {
        is ApiResult.Success -> data
        is ApiResult.Error -> error(message)
    }
}
