package crocalert.app.shared

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.AlertRemoteDataSourceImpl
import crocalert.app.shared.data.repository.AlertRepositoryImpl
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.HttpClientFactory

/**
 * Creates the full AlertRepository wired up to the network layer.
 * Hides Ktor's HttpClient from consumers — only the clean domain interface is returned.
 * Call ApiRoutes.BASE = "..." from each platform entry point before using this.
 */
fun createAlertRepository(): AlertRepository {
    val remote = AlertRemoteDataSourceImpl(HttpClientFactory.create())
    return AlertRepositoryImpl(remote)
}

/**
 * Fetches the current list of alerts directly (single shot, no Flow).
 * Use this from the UI for load-on-demand patterns.
 * Throws on network or server error.
 */
suspend fun fetchAlerts(): List<Alert> {
    val remote = AlertRemoteDataSourceImpl(HttpClientFactory.create())
    return when (val result = remote.getAlerts()) {
        is ApiResult.Success -> result.data.map { it.toModel() }
        is ApiResult.Error -> throw Exception(result.message)
    }
}
