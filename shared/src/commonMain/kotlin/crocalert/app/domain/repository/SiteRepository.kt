package crocalert.app.domain.repository
import crocalert.app.model.Site
import kotlinx.coroutines.flow.Flow

interface SiteRepository {
    fun observeSites(): Flow<List<Site>>
    fun observeSite(siteId: String): Flow<Site?>

    suspend fun createSite(site: Site): String
    suspend fun updateSite(site: Site)
    suspend fun deleteSite(siteId: String)
}