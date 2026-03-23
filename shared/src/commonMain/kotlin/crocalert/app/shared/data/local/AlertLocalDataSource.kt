package crocalert.app.shared.data.local

import crocalert.app.shared.data.dto.AlertDto
import kotlinx.coroutines.flow.Flow

interface AlertLocalDataSource {
    fun selectAll(): Flow<List<AlertDto>>
    suspend fun upsertAll(alerts: List<AlertDto>)
    suspend fun lastSyncedAt(): Long?
    suspend fun latestCreatedAt(): Long?
}
