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
 * Tests for graceful server-side error handling (R-05).
 *
 * Simulates backend failures (Firebase / service layer throwing) and verifies:
 * - Routes return 500 with JSON body (not 500 with plain text or crash).
 * - Error response body contains an "error" field.
 * - All HTTP methods degrade gracefully under service failure.
 */
@IntegrationTest
class ServerErrorHandlingTest {

    private class FailingAlertService(
        private val message: String = "Firebase unavailable"
    ) : AlertServicePort {
        override suspend fun getAll(): List<AlertDto> = error(message)
        override suspend fun getById(id: String): AlertDto? = error(message)
        override suspend fun create(dto: AlertDto): String = error(message)
        override suspend fun update(id: String, dto: AlertDto): Boolean = error(message)
        override suspend fun delete(id: String): Boolean = error(message)
    }

    private fun testApp(
        service: AlertServicePort = FailingAlertService(),
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureRouting(alertService = service)
        }
        block()
    }

    // ── GET /alerts ───────────────────────────────────────────────────────────

    @Test
    fun `GET alerts - service failure returns 500`() = testApp {
        val response = client.get("/alerts")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `GET alerts - service failure response body is JSON with error field`() = testApp {
        val response = client.get("/alerts")
        val body = response.bodyAsText()
        assertTrue(body.contains("\"error\""), "Expected JSON error field, got: $body")
        assertTrue(body.contains("Firebase unavailable"), "Expected cause message, got: $body")
    }

    @Test
    fun `GET alerts - service failure has JSON content-type`() = testApp {
        val response = client.get("/alerts")
        assertTrue(
            response.contentType()?.match(ContentType.Application.Json) == true,
            "Expected JSON content-type on 500, got: ${response.contentType()}"
        )
    }

    // ── POST /alerts ──────────────────────────────────────────────────────────

    @Test
    fun `POST alerts - service failure returns 500`() = testApp {
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AlertDto(title = "fire-test")))
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `POST alerts - service failure response body is JSON`() = testApp {
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AlertDto(title = "fire-test")))
        }
        assertTrue(response.bodyAsText().contains("\"error\""))
    }

    // ── GET /alerts/{id} ──────────────────────────────────────────────────────

    @Test
    fun `GET alerts by id - service failure returns 500`() = testApp {
        val response = client.get("/alerts/some-id")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    // ── PUT /alerts/{id} ──────────────────────────────────────────────────────

    @Test
    fun `PUT alerts by id - service failure returns 500`() = testApp {
        val response = client.put("/alerts/some-id") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AlertDto(title = "update")))
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    // ── DELETE /alerts/{id} ───────────────────────────────────────────────────

    @Test
    fun `DELETE alerts by id - service failure returns 500`() = testApp {
        val response = client.delete("/alerts/some-id")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `service exception message is included in 500 response body`() = testApp(
        FailingAlertService("Firestore quota exceeded")
    ) {
        val response = client.get("/alerts")
        val body = response.bodyAsText()
        assertTrue(body.contains("Firestore quota exceeded"), "Custom message not in body: $body")
    }

    @Test
    fun `GET slash still returns 200 even when alert service fails`() = testApp {
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
