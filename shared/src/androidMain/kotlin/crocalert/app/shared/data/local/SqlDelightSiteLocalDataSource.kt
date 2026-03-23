package crocalert.app.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import crocalert.app.db.SiteQueries
import crocalert.app.shared.data.dto.SiteDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class SqlDelightSiteLocalDataSource(
    private val queries: SiteQueries,
) : SiteLocalDataSource {

    override fun selectAll(): Flow<List<SiteDto>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    SiteDto(
                        id       = row.id,
                        code     = row.code,
                        name     = row.name,
                        isActive = row.is_active != 0L,
                    )
                }
            }

    override suspend fun upsertAll(sites: List<SiteDto>) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            sites.forEach { dto ->
                queries.upsertAll(
                    id        = dto.id,
                    code      = dto.code,
                    name      = dto.name,
                    is_active = if (dto.isActive) 1L else 0L,
                    synced_at = now,
                )
            }
        }
    }

    override suspend fun lastSyncedAt(): Long? = withContext(Dispatchers.IO) {
        queries.lastSyncedAt().executeAsOneOrNull()?.MAX
    }
}
