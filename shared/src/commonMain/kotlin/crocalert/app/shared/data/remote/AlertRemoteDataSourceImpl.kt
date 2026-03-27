package crocalert.app.shared.data.remote

import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.data.dto.IdResponse
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.safeCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AlertRemoteDataSourceImpl(
    private val client: HttpClient,
    baseUrl: String,
    private val apiKey: String = ApiRoutes.API_KEY,
) : AlertRemoteDataSource {

    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
    }

    private val alertsUrl = ApiRoutes.alertsUrl(baseUrl)

    // Adds X-API-Key header when the key is configured (server auth guard enabled)
    private fun HttpRequestBuilder.addAuth() {
        if (apiKey.isNotBlank()) header("X-API-Key", apiKey)
    }

    override suspend fun getAlerts(since: Long?): ApiResult<List<AlertDto>> =
        safeCall {
            client.get(alertsUrl) {
                addAuth()
                if (since != null) parameter("since", since)
            }.body()
        }

    override suspend fun getAlert(id: String): ApiResult<AlertDto> =
        safeCall { client.get("$alertsUrl/$id") { addAuth() }.body() }

    override suspend fun createAlert(dto: AlertDto): ApiResult<IdResponse> =
        safeCall {
            client.post(alertsUrl) {
                addAuth()
                contentType(ContentType.Application.Json)
                setBody(dto)
            }.body<IdResponse>()
        }

    override suspend fun updateAlert(id: String, dto: AlertDto): ApiResult<Unit> =
        safeCall {
            client.put("$alertsUrl/$id") {
                addAuth()
                contentType(ContentType.Application.Json)
                setBody(dto)
            }.body<Unit>()
        }

    override suspend fun deleteAlert(id: String): ApiResult<Unit> =
        safeCall { client.delete("$alertsUrl/$id") { addAuth() }.body<Unit>() }
}