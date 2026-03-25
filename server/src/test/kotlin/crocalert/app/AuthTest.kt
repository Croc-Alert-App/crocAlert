package crocalert.app

import crocalert.app.shared.data.dto.AlertDto
import crocalert.server.service.AlertServicePort
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for [configureAuth] middleware (R-09).
 *
 * Covers:
 * - Dev mode (blank key) — all routes pass through without a header.
 * - Auth enforced — valid key accepted, invalid/missing key → 401.
 * - GET / is always exempt from auth.
 * - All HTTP methods are protected (GET, POST, PUT, DELETE).
 */
@IntegrationTest
class AuthTest {

    private val validKey = "test-api-key-1234"

    private val emptyAlertService = object : AlertServicePort {
        override suspend fun getAll(since: Long?) = emptyList<AlertDto>()
        override suspend fun getById(id: String) = null
        override suspend fun create(dto: AlertDto) = "new-id"
        override suspend fun update(id: String, dto: AlertDto) = false
        override suspend fun delete(id: String) = false
    }

    // ── Helper builders ───────────────────────────────────────────────────────

    /** App with auth enforced using [validKey]. */
    private fun authApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            configureSerialization()
            configureAuth(apiKey = validKey)
            configureRouting(alertService = emptyAlertService)
        }
        block()
    }

    /** App in dev mode (blank key = no auth). */
    private fun devApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            configureSerialization()
            configureAuth(apiKey = "")   // blank → dev mode
            configureRouting(alertService = emptyAlertService)
        }
        block()
    }

    // ── Dev mode (no auth) ────────────────────────────────────────────────────

    @Test
    fun `dev mode - GET alerts succeeds without any API key`() = devApp {
        val response = client.get("/alerts")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `dev mode - POST alerts succeeds without any API key`() = devApp {
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AlertDto(title = "test")))
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    // ── Auth enforced — GET / always passes ───────────────────────────────────

    @Test
    fun `auth enforced - GET slash is exempt and always returns 200`() = authApp {
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Server running", response.bodyAsText())
    }

    @Test
    fun `auth enforced - GET slash is exempt even without a key header`() = authApp {
        // Explicitly confirm no X-API-Key header is sent
        val response = client.get("/") { headers.remove("X-API-Key") }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // ── Auth enforced — valid key ─────────────────────────────────────────────

    @Test
    fun `auth enforced - GET alerts with valid key returns 200`() = authApp {
        val response = client.get("/alerts") {
            header("X-API-Key", validKey)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `auth enforced - POST alerts with valid key returns 201`() = authApp {
        val response = client.post("/alerts") {
            header("X-API-Key", validKey)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AlertDto(title = "auth-test")))
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `auth enforced - PUT alerts with valid key returns 404 on missing id (not 401)`() = authApp {
        val response = client.put("/alerts/ghost") {
            header("X-API-Key", validKey)
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AlertDto(title = "x")))
        }
        // 404 means auth passed; the resource just doesn't exist
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `auth enforced - DELETE alerts with valid key returns 404 on missing id (not 401)`() = authApp {
        val response = client.delete("/alerts/ghost") {
            header("X-API-Key", validKey)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── Auth enforced — missing key ───────────────────────────────────────────

    @Test
    fun `auth enforced - GET alerts without key returns 401`() = authApp {
        val response = client.get("/alerts")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `auth enforced - POST alerts without key returns 401`() = authApp {
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AlertDto(title = "no-key")))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `auth enforced - PUT alerts without key returns 401`() = authApp {
        val response = client.put("/alerts/any-id") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AlertDto(title = "no-key")))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `auth enforced - DELETE alerts without key returns 401`() = authApp {
        val response = client.delete("/alerts/any-id")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ── Auth enforced — invalid key ───────────────────────────────────────────

    @Test
    fun `auth enforced - GET alerts with wrong key returns 401`() = authApp {
        val response = client.get("/alerts") {
            header("X-API-Key", "wrong-key")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `auth enforced - GET alerts with empty key value returns 401`() = authApp {
        val response = client.get("/alerts") {
            header("X-API-Key", "")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `auth enforced - 401 response body contains descriptive message`() = authApp {
        val response = client.get("/alerts")
        val body = response.bodyAsText()
        assertTrue(body.isNotBlank(), "401 response should have a non-empty body")
        assertTrue(
            body.contains("X-API-Key", ignoreCase = true) || body.contains("Invalid", ignoreCase = true),
            "401 body should mention the key or be descriptive, got: $body"
        )
    }
}
