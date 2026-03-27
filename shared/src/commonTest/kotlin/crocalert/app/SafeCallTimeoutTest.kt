package crocalert.app

import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.safeCall
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Supplementary timeout and network-error tests for [safeCall] (R-10).
 *
 * Verifies that request-timeout and connection-error scenarios are
 * captured as [ApiResult.Error] — not as unhandled exceptions — across
 * all HTTP operations used by the data layer.
 */
@UnitTest
class SafeCallTimeoutTest {

    // ── Request-timeout-like exceptions ───────────────────────────────────────

    @Test
    fun `safeCall wraps IllegalStateException (simulated request timeout) as Error`() = runTest {
        // HttpRequestTimeoutException extends IllegalStateException in Ktor 2.x
        val result = safeCall<Unit> {
            throw IllegalStateException(
                "Request timeout has expired [url=http://10.0.2.2:8080/alerts, timeout=5000 ms]"
            )
        }
        assertIs<ApiResult.Error>(result)
        assertTrue(
            result.message.contains("timeout", ignoreCase = true),
            "Error message should reference timeout, got: ${result.message}"
        )
        assertNull(result.code, "Timeout errors have no HTTP status code")
    }

    @Test
    fun `safeCall wraps ConnectTimeoutException-like IOException as Network error`() = runTest {
        val result = safeCall<Unit> {
            throw io.ktor.utils.io.errors.IOException("Connection timed out after 10000ms")
        }
        assertIs<ApiResult.Error>(result)
        assertTrue(result.message.startsWith("Network error"), "Expected 'Network error' prefix")
        assertTrue(result.message.contains("timed out", ignoreCase = true))
        assertNull(result.code)
    }

    // ── Repeated calls remain independent ────────────────────────────────────

    @Test
    fun `safeCall on second call succeeds after first call failed with IOException`() = runTest {
        var callCount = 0
        val result = safeCall {
            callCount++
            if (callCount == 1) throw io.ktor.utils.io.errors.IOException("first call fails")
            "recovered"
        }
        // Only one call made here — caller must retry; each safeCall is independent
        assertIs<ApiResult.Error>(result)

        val retryResult = safeCall { "recovered" }
        assertIs<ApiResult.Success<String>>(retryResult)
        assertEquals("recovered", retryResult.data)
    }

    // ── Mock engine — connection refused ──────────────────────────────────────

    @Test
    fun `safeCall with mock engine returning 408 Request Timeout returns Error with code 408`() = runTest {
        val client = buildMockClient(HttpStatusCode(408, "Request Timeout"))
        val result = safeCall { client.get("http://test/alerts"); Unit }
        assertIs<ApiResult.Error>(result)
        assertEquals(408, result.code)
    }

    @Test
    fun `safeCall with mock engine returning 503 Service Unavailable returns Error with code 503`() = runTest {
        val client = buildMockClient(HttpStatusCode.ServiceUnavailable)
        val result = safeCall { client.get("http://test/alerts"); Unit }
        assertIs<ApiResult.Error>(result)
        assertEquals(503, result.code)
        assertTrue(result.message.contains("503"))
    }

    @Test
    fun `safeCall with mock engine returning 504 Gateway Timeout returns Error with code 504`() = runTest {
        val client = buildMockClient(HttpStatusCode(504, "Gateway Timeout"))
        val result = safeCall { client.get("http://test/alerts"); Unit }
        assertIs<ApiResult.Error>(result)
        assertEquals(504, result.code)
    }

    // ── Error message format contract ─────────────────────────────────────────

    @Test
    fun `safeCall IOException message includes Network error prefix`() = runTest {
        val result = safeCall<String> {
            throw io.ktor.utils.io.errors.IOException("socket reset by peer")
        }
        assertIs<ApiResult.Error>(result)
        assertTrue(
            result.message.startsWith("Network error:"),
            "IOException must produce 'Network error:' prefix, got: ${result.message}"
        )
    }

    @Test
    fun `safeCall 5xx error message includes status code`() = runTest {
        val client = buildMockClient(HttpStatusCode.InternalServerError)
        val result = safeCall { client.get("http://test/alerts"); Unit }
        assertIs<ApiResult.Error>(result)
        assertTrue(result.message.contains("500"), "5xx message must include status code")
    }

    @Test
    fun `safeCall 4xx error message includes status code`() = runTest {
        val client = buildMockClient(HttpStatusCode.NotFound)
        val result = safeCall { client.get("http://test/alerts"); Unit }
        assertIs<ApiResult.Error>(result)
        assertTrue(result.message.contains("404"), "4xx message must include status code")
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
