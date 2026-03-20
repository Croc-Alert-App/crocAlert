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
 * Error-shape tests for the alerts REST API.
 *
 * Covers: malformed request bodies (400), missing resources (404),
 * and response Content-Type validation.
 */
class AlertsRouteErrorTest {

    // ── Fake service ──────────────────────────────────────────────────────────

    private class FakeAlertService(
        private val alerts: MutableList<AlertDto> = mutableListOf()
    ) : AlertServicePort {
        override suspend fun getAll() = alerts.toList()
        override suspend fun getById(id: String) = alerts.firstOrNull { it.id == id }
        override suspend fun create(dto: AlertDto): String {
            val id = "generated-id"
            alerts += dto.copy(id = id)
            return id
        }
        override suspend fun update(id: String, dto: AlertDto): Boolean {
            val idx = alerts.indexOfFirst { it.id == id }
            if (idx == -1) return false
            alerts[idx] = dto.copy(id = id)
            return true
        }
        override suspend fun delete(id: String): Boolean = alerts.removeIf { it.id == id }
    }

    // ── Test application builder ───────────────────────────────────────────────

    private fun testApp(
        service: AlertServicePort = FakeAlertService(),
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureRouting(alertService = service)
        }
        block()
    }

    // ── 400 Bad Request — malformed body ──────────────────────────────────────

    @Test
    fun `POST alerts with malformed JSON returns 400`() = testApp {
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody("{ this is not valid json }")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST alerts with empty body returns 400`() = testApp {
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody("")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT alerts with malformed JSON returns 400`() = testApp(
        FakeAlertService(mutableListOf(AlertDto(id = "a1", title = "Existing")))
    ) {
        val response = client.put("/alerts/a1") {
            contentType(ContentType.Application.Json)
            setBody("not json at all")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ── 404 Not Found — response body ─────────────────────────────────────────

    @Test
    fun `GET alerts by unknown id returns 404 with no crash`() = testApp(FakeAlertService()) {
        val response = client.get("/alerts/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT alerts on non-existent id returns 404`() = testApp(FakeAlertService()) {
        val body = Json.encodeToString(AlertDto(title = "ghost"))
        val response = client.put("/alerts/ghost-id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE alerts on non-existent id returns 404`() = testApp(FakeAlertService()) {
        val response = client.delete("/alerts/ghost-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── Content-Type validation in responses ──────────────────────────────────

    @Test
    fun `GET alerts response has JSON content-type`() = testApp(
        FakeAlertService(mutableListOf(AlertDto(id = "a1", title = "Alert")))
    ) {
        val response = client.get("/alerts")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(
            response.contentType()?.match(ContentType.Application.Json) == true,
            "Expected JSON content-type, got ${response.contentType()}"
        )
    }

    @Test
    fun `POST alerts success response has JSON content-type`() = testApp {
        val body = Json.encodeToString(AlertDto(title = "New"))
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(
            response.contentType()?.match(ContentType.Application.Json) == true,
            "Expected JSON content-type, got ${response.contentType()}"
        )
    }

    @Test
    fun `GET alerts by id response has JSON content-type`() = testApp(
        FakeAlertService(mutableListOf(AlertDto(id = "a1", title = "Found")))
    ) {
        val response = client.get("/alerts/a1")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(
            response.contentType()?.match(ContentType.Application.Json) == true,
            "Expected JSON content-type, got ${response.contentType()}"
        )
    }

    // ── Response body shape validation ────────────────────────────────────────

    @Test
    fun `POST alerts response body contains the generated id field`() = testApp {
        val body = Json.encodeToString(AlertDto(title = "Shape check"))
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val bodyText = response.bodyAsText()
        assertTrue(bodyText.contains("\"id\""), "Response body must contain \"id\" key, got: $bodyText")
    }

    @Test
    fun `GET alerts list response body is a JSON array`() = testApp(FakeAlertService()) {
        val response = client.get("/alerts")
        val bodyText = response.bodyAsText().trim()
        assertTrue(bodyText.startsWith("["), "Expected JSON array, got: $bodyText")
        assertTrue(bodyText.endsWith("]"), "Expected JSON array, got: $bodyText")
    }

    @Test
    fun `GET alerts by id response body contains the alert id`() = testApp(
        FakeAlertService(mutableListOf(AlertDto(id = "shape-id", title = "Shape")))
    ) {
        val response = client.get("/alerts/shape-id")
        val bodyText = response.bodyAsText()
        assertTrue(bodyText.contains("shape-id"), "Response body must contain alert id, got: $bodyText")
    }
}
