package crocalert.app.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import crocalert.app.db.AlertQueries
import crocalert.app.shared.data.dto.AlertDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class SqlDelightAlertLocalDataSource(
    private val queries: AlertQueries,
) : AlertLocalDataSource {

    override fun selectAll(): Flow<List<AlertDto>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    AlertDto(
                        id            = row.id,
                        captureId     = row.capture_id,
                        cameraId      = row.camera_id,
                        aiConfidence  = row.ai_confidence?.toFloat(),
                        createdAt     = row.created_at,
                        status        = row.status,
                        priority      = row.priority,
                        title         = row.title,
                        folder        = row.folder,
                    )
                }
            }

    override suspend fun upsertAll(alerts: List<AlertDto>) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            alerts.forEach { dto ->
                queries.upsertAll(
                    id            = dto.id,
                    capture_id    = dto.captureId,
                    camera_id     = dto.cameraId,
                    ai_confidence = dto.aiConfidence?.toDouble(),
                    created_at    = dto.createdAt,
                    status        = dto.status,
                    priority      = dto.priority,
                    title         = dto.title,
                    folder        = dto.folder,
                    synced_at     = now,
                )
            }
        }
    }

    override suspend fun lastSyncedAt(): Long? = withContext(Dispatchers.IO) {
        queries.lastSyncedAt().executeAsOneOrNull()?.MAX
    }

    override suspend fun latestCreatedAt(): Long? = withContext(Dispatchers.IO) {
        queries.latestCreatedAt().executeAsOneOrNull()?.MAX
    }
}
