package crocalert.app

import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.data.dto.IdResponse
import crocalert.app.shared.data.remote.AlertRemoteDataSourceImpl
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.ApiRoutes
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class AlertRemoteDataSourceImplTest {

    private val baseUrl = "http://test"
    private val alertsUrl = "$baseUrl/alerts"

    private val sampleDto = AlertDto(
        id = "alert-1",
        captureId = "cap-1",
        createdAt = 1000L,
        status = "OPEN",
        priority = "HIGH",
        title = "Test alert"
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildClient(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "[]",
        capturedHeaders: MutableList<Map<String, List<String>>> = mutableListOf()
    ): HttpClient {
        val engine = MockEngine { request ->
            capturedHeaders += request.headers.entries().associate { it.key to it.value }
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
        }
    }

    private fun impl(client: HttpClient) = AlertRemoteDataSourceImpl(client, baseUrl)

    // ── getAlerts ─────────────────────────────────────────────────────────────

    @Test
    fun `getAlerts returns Success with empty list`() = runTest {
        val result = impl(buildClient(body = "[]")).getAlerts()
        assertIs<ApiResult.Success<List<AlertDto>>>(result)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `getAlerts returns Success with mapped list`() = runTest {
        val body = Json.encodeToString(listOf(sampleDto))
        val result = impl(buildClient(body = body)).getAlerts()
        assertIs<ApiResult.Success<List<AlertDto>>>(result)
        assertEquals(1, result.data.size)
        assertEquals("alert-1", result.data[0].id)
        assertEquals("HIGH", result.data[0].priority)
    }

    @Test
    fun `getAlerts returns Error on 401`() = runTest {
        val result = impl(buildClient(status = HttpStatusCode.Unauthorized, body = "Unauthorized")).getAlerts()
        assertIs<ApiResult.Error>(result)
        assertEquals(401, result.code)
    }

    @Test
    fun `getAlerts returns Error on 500`() = runTest {
        val result = impl(buildClient(status = HttpStatusCode.InternalServerError, body = "Server Error")).getAlerts()
        assertIs<ApiResult.Error>(result)
        assertEquals(500, result.code)
    }

    // ── getAlert ──────────────────────────────────────────────────────────────

    @Test
    fun `getAlert returns Success with matching alert`() = runTest {
        val body = Json.encodeToString(sampleDto)
        val result = impl(buildClient(body = body)).getAlert("alert-1")
        assertIs<ApiResult.Success<AlertDto>>(result)
        assertEquals("alert-1", result.data.id)
    }

    @Test
    fun `getAlert returns Error on 404`() = runTest {
        val result = impl(buildClient(status = HttpStatusCode.NotFound, body = "Not Found")).getAlert("missing")
        assertIs<ApiResult.Error>(result)
        assertEquals(404, result.code)
    }

    // ── createAlert ───────────────────────────────────────────────────────────

    @Test
    fun `createAlert returns Success with id from response`() = runTest {
        val body = """{"id":"server-generated-id"}"""
        val result = impl(buildClient(status = HttpStatusCode.Created, body = body)).createAlert(sampleDto)
        assertIs<ApiResult.Success<IdResponse>>(result)
        assertEquals("server-generated-id", result.data.id)
    }

    @Test
    fun `createAlert returns Error on 400`() = runTest {
        val result = impl(buildClient(status = HttpStatusCode.BadRequest, body = "Bad request")).createAlert(sampleDto)
        assertIs<ApiResult.Error>(result)
        assertEquals(400, result.code)
    }

    // ── updateAlert ───────────────────────────────────────────────────────────

    @Test
    fun `updateAlert returns Success on 200`() = runTest {
        val result = impl(buildClient(status = HttpStatusCode.OK, body = "")).updateAlert("alert-1", sampleDto)
        assertIs<ApiResult.Success<Unit>>(result)
    }

    @Test
    fun `updateAlert returns Error on 404`() = runTest {
        val result = impl(buildClient(status = HttpStatusCode.NotFound, body = "Not Found")).updateAlert("missing", sampleDto)
        assertIs<ApiResult.Error>(result)
        assertEquals(404, result.code)
    }

    // ── deleteAlert ───────────────────────────────────────────────────────────

    @Test
    fun `deleteAlert returns Success on 200`() = runTest {
        val result = impl(buildClient(status = HttpStatusCode.OK, body = "")).deleteAlert("alert-1")
        assertIs<ApiResult.Success<Unit>>(result)
    }

    @Test
    fun `deleteAlert returns Error on 404`() = runTest {
        val result = impl(buildClient(status = HttpStatusCode.NotFound, body = "Not Found")).deleteAlert("missing")
        assertIs<ApiResult.Error>(result)
        assertEquals(404, result.code)
    }

    // ── Auth header ───────────────────────────────────────────────────────────

    @Test
    fun `X-API-Key header is added when API_KEY is set`() = runTest {
        val captured = mutableListOf<Map<String, List<String>>>()
        val client = buildClient(capturedHeaders = captured)
        ApiRoutes.API_KEY = "secret-key"
        try {
            impl(client).getAlerts()
            assertEquals("secret-key", captured.first()["X-API-Key"]?.first())
        } finally {
            ApiRoutes.API_KEY = ""
        }
    }

    @Test
    fun `X-API-Key header is absent when API_KEY is blank`() = runTest {
        val captured = mutableListOf<Map<String, List<String>>>()
        val client = buildClient(capturedHeaders = captured)
        ApiRoutes.API_KEY = ""
        impl(client).getAlerts()
        assertNull(captured.first()["X-Api-Key"])
    }
}
