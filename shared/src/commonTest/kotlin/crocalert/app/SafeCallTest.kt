package crocalert.app

import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.safeCall
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SafeCallTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `returns Success when block completes normally`() = runTest {
        val result = safeCall { 42 }
        assertIs<ApiResult.Success<Int>>(result)
        assertEquals(42, result.data)
    }

    @Test
    fun `returns Success with null value`() = runTest {
        val result = safeCall<String?> { null }
        assertIs<ApiResult.Success<String?>>(result)
        assertNull(result.data)
    }

    // ── IOException ───────────────────────────────────────────────────────────

    @Test
    fun `wraps IOException as Error with no http code`() = runTest {
        val result = safeCall<Int> { throw IOException("connection timed out") }
        assertIs<ApiResult.Error>(result)
        assertTrue(result.message.contains("Network error"), "Expected 'Network error' in: ${result.message}")
        assertTrue(result.message.contains("connection timed out"), "Expected original message in: ${result.message}")
        assertNull(result.code)
    }

    // ── ClientRequestException (4xx via MockEngine) ───────────────────────────

    @Test
    fun `wraps ClientRequestException as Error with 4xx code`() = runTest {
        val client = buildMockClient(HttpStatusCode.NotFound)
        val result = safeCall { client.get("http://test/resource"); Unit }
        assertIs<ApiResult.Error>(result)
        assertEquals(404, result.code)
        assertTrue(result.message.contains("404"), "Expected code in message: ${result.message}")
    }

    @Test
    fun `wraps ClientRequestException as Error with 401 code`() = runTest {
        val client = buildMockClient(HttpStatusCode.Unauthorized)
        val result = safeCall { client.get("http://test/resource"); Unit }
        assertIs<ApiResult.Error>(result)
        assertEquals(401, result.code)
    }

    // ── ServerResponseException (5xx via MockEngine) ──────────────────────────

    @Test
    fun `wraps ServerResponseException as Error with 5xx code`() = runTest {
        val client = buildMockClient(HttpStatusCode.InternalServerError)
        val result = safeCall { client.get("http://test/resource"); Unit }
        assertIs<ApiResult.Error>(result)
        assertEquals(500, result.code)
        assertTrue(result.message.contains("500"), "Expected code in message: ${result.message}")
    }

    @Test
    fun `wraps ServerResponseException as Error with 503 code`() = runTest {
        val client = buildMockClient(HttpStatusCode.ServiceUnavailable)
        val result = safeCall { client.get("http://test/resource"); Unit }
        assertIs<ApiResult.Error>(result)
        assertEquals(503, result.code)
    }

    // ── Generic Exception ─────────────────────────────────────────────────────

    @Test
    fun `wraps generic Exception as Error with no http code`() = runTest {
        val result = safeCall<Int> { throw IllegalStateException("unexpected state") }
        assertIs<ApiResult.Error>(result)
        assertEquals("unexpected state", result.message)
        assertNull(result.code)
    }

    @Test
    fun `wraps generic Exception with null message as Unknown error`() = runTest {
        val result = safeCall<Int> { throw RuntimeException(null as String?) }
        assertIs<ApiResult.Error>(result)
        assertEquals("Unknown error", result.message)
        assertNull(result.code)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildMockClient(status: HttpStatusCode): HttpClient {
        val engine = MockEngine { _ ->
            respond(
                content = status.description,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }
        return HttpClient(engine) { expectSuccess = true }
    }
}
