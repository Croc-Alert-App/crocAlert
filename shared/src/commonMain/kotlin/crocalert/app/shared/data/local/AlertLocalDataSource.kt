package crocalert.app.shared.data.local

import crocalert.app.shared.data.dto.AlertDto
import kotlinx.coroutines.flow.Flow

interface AlertLocalDataSource {
    fun selectAll(): Flow<List<AlertDto>>
    /** Incremental sync: merges [alerts] into existing cache by id. Preserves records not in [alerts]. */
    suspend fun upsertAll(alerts: List<AlertDto>)
    /** Full sync: replaces entire cache with [alerts], evicting any records absent from the server response. */
    suspend fun clearAndUpsertAll(alerts: List<AlertDto>)
    suspend fun lastSyncedAt(): Long?
    suspend fun latestCreatedAt(): Long?
}
