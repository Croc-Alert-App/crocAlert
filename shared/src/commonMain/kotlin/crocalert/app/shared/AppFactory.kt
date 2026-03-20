package crocalert.app.shared

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.domain.repository.SiteRepository
import crocalert.app.shared.data.remote.AlertRemoteDataSourceImpl
import crocalert.app.shared.data.remote.CameraRemoteDataSourceImpl
import crocalert.app.shared.data.remote.SiteRemoteDataSourceImpl
import crocalert.app.shared.data.repository.AlertRepositoryImpl
import crocalert.app.shared.data.repository.CameraRepositoryImpl
import crocalert.app.shared.data.repository.SiteRepositoryImpl
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.HttpClientFactory

// Singleton HTTP client — shared across all repository instances to reuse the connection pool.
private val sharedHttpClient by lazy { HttpClientFactory.create() }

fun createAlertRepository(baseUrl: String = ApiRoutes.DEFAULT_BASE): AlertRepository {
    val remote = AlertRemoteDataSourceImpl(sharedHttpClient, baseUrl)
    return AlertRepositoryImpl(remote)
}

fun createCameraRepository(baseUrl: String = ApiRoutes.DEFAULT_BASE): CameraRepository {
    val remote = CameraRemoteDataSourceImpl(sharedHttpClient, baseUrl)
    return CameraRepositoryImpl(remote)
}

fun createSiteRepository(baseUrl: String = ApiRoutes.DEFAULT_BASE): SiteRepository {
    val remote = SiteRemoteDataSourceImpl(sharedHttpClient, baseUrl)
    return SiteRepositoryImpl(remote)
}
