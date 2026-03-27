package crocalert.app.shared.data.repository

import crocalert.app.domain.repository.SiteRepository
import crocalert.app.model.Site
import crocalert.app.shared.data.local.SiteLocalDataSource
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.SiteRemoteDataSource
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.sync.SyncPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

class SiteRepositoryImpl(
    private val remote: SiteRemoteDataSource,
    private val local: SiteLocalDataSource,
    private val syncPrefs: Flow<SyncPreferences> = kotlinx.coroutines.flow.flowOf(SyncPreferences()),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : SiteRepository {

    override fun observeSites(): Flow<List<Site>> =
        local.selectAll()
            .map { dtos -> dtos.map { it.toModel() } }
            .also { coroutineScope.launch { syncIfStale() } }

    override fun observeSite(siteId: String): Flow<Site?> =
        local.selectAll()
            .map { dtos -> dtos.firstOrNull { it.id == siteId }?.toModel() }
            .also { coroutineScope.launch { syncIfStale() } }

    override suspend fun createSite(site: Site): String = error("not implemented")
    override suspend fun updateSite(site: Site) = error("not implemented")
    override suspend fun deleteSite(siteId: String) = error("not implemented")

    internal suspend fun forceSync() = sync()

    private suspend fun syncIfStale() {
        val ttl = syncPrefs.first().sitesTtlMinutes
        val staleThreshold = Clock.System.now().minus(ttl.minutes).toEpochMilliseconds()
        val lastSync = local.lastSyncedAt() ?: 0L
        if (lastSync < staleThreshold) sync()
    }

    private suspend fun sync() {
        when (val res = remote.getSites()) {
            is ApiResult.Success -> local.upsertAll(res.data)
            is ApiResult.Error -> { /* stale data retained */ }
        }
    }
}
