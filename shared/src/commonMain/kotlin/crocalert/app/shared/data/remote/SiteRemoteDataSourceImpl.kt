package crocalert.app.shared.data.remote

import crocalert.app.shared.data.dto.SiteDto
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.safeCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header

class SiteRemoteDataSourceImpl(
    private val client: HttpClient,
    private val baseUrl: String
) : SiteRemoteDataSource {

    private val sitesUrl = ApiRoutes.sitesUrl(baseUrl)

    private fun HttpRequestBuilder.addAuth() {
        val key = ApiRoutes.API_KEY
        if (key.isNotBlank()) header("X-API-Key", key)
    }

    override suspend fun getSites(): ApiResult<List<SiteDto>> =
        safeCall { client.get(sitesUrl) { addAuth() }.body() }
}
