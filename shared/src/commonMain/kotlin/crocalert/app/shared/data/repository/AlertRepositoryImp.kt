package crocalert.app.shared.data.remote

import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.safeCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AlertRemoteDataSourceImpl(
    private val client: HttpClient
) : AlertRemoteDataSource {

    override suspend fun getAlerts(): ApiResult<List<AlertDto>> =
        safeCall {
            client.get(ApiRoutes.ALERTS).body()
        }

    override suspend fun getAlert(id: String): ApiResult<AlertDto> =
        safeCall {
            client.get("${ApiRoutes.ALERTS}/$id").body()
        }

    override suspend fun createAlert(dto: AlertDto): ApiResult<String> =
        safeCall {
            client.post(ApiRoutes.ALERTS) {
                contentType(ContentType.Application.Json)
                setBody(dto)
            }.body()
        }

    override suspend fun updateAlert(id: String, dto: AlertDto): ApiResult<Unit> =
        safeCall {
            client.put("${ApiRoutes.ALERTS}/$id") {
                contentType(ContentType.Application.Json)
                setBody(dto)
            }
        }

    override suspend fun deleteAlert(id: String): ApiResult<Unit> =
        safeCall {
            client.delete("${ApiRoutes.ALERTS}/$id")
        }
}