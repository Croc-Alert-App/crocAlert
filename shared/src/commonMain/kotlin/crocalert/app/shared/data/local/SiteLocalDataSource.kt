package crocalert.app.shared.data.local

import crocalert.app.shared.data.dto.SiteDto
import kotlinx.coroutines.flow.Flow

interface SiteLocalDataSource {
    fun selectAll(): Flow<List<SiteDto>>
    suspend fun upsertAll(sites: List<SiteDto>)
    suspend fun lastSyncedAt(): Long?
}
