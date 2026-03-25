package crocalert.app.shared.data.local

import crocalert.app.shared.data.dto.SiteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

class InMemorySiteLocalDataSource : SiteLocalDataSource {

    private val _sites = MutableStateFlow<List<SiteDto>>(emptyList())
    private var _lastSyncedAt: Long? = null

    override fun selectAll(): Flow<List<SiteDto>> = _sites

    override suspend fun upsertAll(sites: List<SiteDto>) {
        _sites.value = sites
        _lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun lastSyncedAt(): Long? = _lastSyncedAt
}
