package crocalert.app.shared.data.local

import crocalert.app.shared.data.dto.AlertDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

class InMemoryAlertLocalDataSource : AlertLocalDataSource {

    private val _alerts = MutableStateFlow<List<AlertDto>>(emptyList())
    private var _lastSyncedAt: Long? = null

    override fun selectAll(): Flow<List<AlertDto>> = _alerts

    override suspend fun upsertAll(alerts: List<AlertDto>) {
        val merged = _alerts.value.associateBy { it.id }.toMutableMap()
        alerts.forEach { merged[it.id] = it }
        _alerts.value = merged.values.sortedByDescending { it.createdAt }
        _lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun clearAndUpsertAll(alerts: List<AlertDto>) {
        _alerts.value = alerts.sortedByDescending { it.createdAt }
        _lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun lastSyncedAt(): Long? = _lastSyncedAt

    override suspend fun latestCreatedAt(): Long? =
        _alerts.value.maxByOrNull { it.createdAt }?.createdAt
}
