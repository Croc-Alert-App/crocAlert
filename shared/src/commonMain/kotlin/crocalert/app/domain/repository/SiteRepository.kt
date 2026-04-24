package crocalert.app.domain.repository
import crocalert.app.model.Site
import kotlinx.coroutines.flow.Flow

interface SiteRepository {
    /** Returns a live stream of all cached sites. */
    fun observeSites(): Flow<List<Site>>

    /** Returns a live stream for a single site; emits null when the site does not exist. */
    fun observeSite(siteId: String): Flow<Site?>

    /** Creates the site on the server and returns the server-generated ID. */
    suspend fun createSite(site: Site): String

    /** Updates site fields on the server. */
    suspend fun updateSite(site: Site)

    /** Deletes the site by ID from the server and removes it from the local cache. */
    suspend fun deleteSite(siteId: String)
}