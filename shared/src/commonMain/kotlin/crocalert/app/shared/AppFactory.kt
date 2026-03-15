package crocalert.app.shared

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.shared.data.remote.AlertRemoteDataSourceImpl
import crocalert.app.shared.data.repository.AlertRepositoryImpl
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.HttpClientFactory

// Singleton HTTP client — shared across all repository instances to reuse the connection pool.
private val sharedHttpClient by lazy { HttpClientFactory.create() }

/**
 * Creates an AlertRepository wired to the given [baseUrl].
 * Reuses the platform's singleton HttpClient — safe to call multiple times.
 *
 * Set [ApiRoutes.API_KEY] before calling this if the server has CROC_API_KEY configured.
 */
fun createAlertRepository(baseUrl: String = ApiRoutes.DEFAULT_BASE): AlertRepository {
    val remote = AlertRemoteDataSourceImpl(sharedHttpClient, baseUrl)
    return AlertRepositoryImpl(remote)
}
