package crocalert.app.shared.data.remote

import crocalert.app.shared.data.dto.CameraDto
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.safeCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CameraRemoteDataSourceImpl(
    private val client: HttpClient,
    private val baseUrl: String
) : CameraRemoteDataSource {

    private val camerasUrl = ApiRoutes.camerasUrl(baseUrl)

    private fun HttpRequestBuilder.addAuth() {
        val key = ApiRoutes.API_KEY
        if (key.isNotBlank()) header("X-API-Key", key)
    }

    override suspend fun getCameras(): ApiResult<List<CameraDto>> =
        safeCall { client.get(camerasUrl) { addAuth() }.body() }

    override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> =
        safeCall { client.get(ApiRoutes.capturesByCameraUrl(baseUrl, cameraId)) { addAuth() }.body() }

    override suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto> =
        safeCall { client.get(ApiRoutes.dailyStatsUrl(baseUrl, cameraId, date)) { addAuth() }.body() }

    override suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>> =
        safeCall { client.get(ApiRoutes.allDailyStatsUrl(baseUrl, date)) { addAuth() }.body() }

    override suspend fun createCamera(dto: CameraDto): ApiResult<String> = safeCall {
        client.post(camerasUrl) {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body<Map<String, String>>()["id"] ?: ""
    }

    override suspend fun updateCamera(id: String, dto: CameraDto): ApiResult<Unit> = safeCall {
        client.put("$camerasUrl/$id") {
            addAuth()
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        Unit
    }
}
