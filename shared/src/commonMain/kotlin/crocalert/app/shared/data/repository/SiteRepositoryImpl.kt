package crocalert.app.shared.data.repository

import crocalert.app.domain.repository.SiteRepository
import crocalert.app.model.Site
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.SiteRemoteDataSource
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class SiteRepositoryImpl(
    private val remote: SiteRemoteDataSource
) : SiteRepository {

    // null = not yet loaded; empty list = server returned zero sites (valid state)
    private val sitesFlow = MutableStateFlow<List<Site>?>(null)

    override fun observeSites(): Flow<List<Site>> = flow {
        ensureLoaded()
        emitAll(sitesFlow.map { it ?: emptyList() })
    }

    override fun observeSite(siteId: String): Flow<Site?> = flow {
        ensureLoaded()
        emitAll(sitesFlow.map { list -> list?.firstOrNull { it.id == siteId } })
    }

    override suspend fun createSite(site: Site): String = error("not implemented")
    override suspend fun updateSite(site: Site) = error("not implemented")
    override suspend fun deleteSite(siteId: String) = error("not implemented")

    private suspend fun ensureLoaded() {
        if (sitesFlow.value == null) refresh()
    }

    // Silently retains stale data on error to avoid killing observers.
    private suspend fun refresh() {
        when (val res = remote.getSites()) {
            is ApiResult.Success -> sitesFlow.value = res.data.map { it.toModel() }
            is ApiResult.Error -> { /* stale data retained */ }
        }
    }
}
