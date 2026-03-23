package crocalert.app.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import crocalert.app.db.CameraQueries
import crocalert.app.shared.data.dto.CameraDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class SqlDelightCameraLocalDataSource(
    private val queries: CameraQueries,
) : CameraLocalDataSource {

    override fun selectAll(): Flow<List<CameraDto>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    CameraDto(
                        id             = row.id,
                        name           = row.name,
                        isActive       = row.is_active != 0L,
                        siteId         = row.site_id,
                        expectedImages = row.expected_images?.toInt(),
                        createdAt      = row.created_at,
                        installedAt    = row.installed_at,
                    )
                }
            }

    override suspend fun upsertAll(cameras: List<CameraDto>) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            cameras.forEach { dto ->
                queries.upsertAll(
                    id              = dto.id,
                    name            = dto.name,
                    is_active       = if (dto.isActive) 1L else 0L,
                    site_id         = dto.siteId,
                    expected_images = dto.expectedImages?.toLong(),
                    created_at      = dto.createdAt,
                    installed_at    = dto.installedAt,
                    synced_at       = now,
                )
            }
        }
    }

    override suspend fun lastSyncedAt(): Long? = withContext(Dispatchers.IO) {
        queries.lastSyncedAt().executeAsOneOrNull()?.MAX
    }
}
