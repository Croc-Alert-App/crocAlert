package crocalert.app

import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.data.dto.IdResponse
import crocalert.server.service.AlertServicePort
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class AlertsRouteTest {

    // ── Fake service ──────────────────────────────────────────────────────────

    private class FakeAlertService(
        private val alerts: MutableList<AlertDto> = mutableListOf()
    ) : AlertServicePort {
        override suspend fun getAll(since: Long?) = alerts.toList()
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
        override suspend fun delete(id: String): Boolean {
            return alerts.removeIf { it.id == id }
        }
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

    // ── Health check ──────────────────────────────────────────────────────────

    @Test
    fun `GET slash returns 200 Server running`() = testApp {
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Server running", response.bodyAsText())
    }

    // ── Routing structure ─────────────────────────────────────────────────────

    @Test
    fun `GET alerts slash empty id returns 400 or 404`() = testApp {
        val response = client.get("/alerts/")
        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.NotFound,
            "Expected 400 or 404 but got ${response.status}"
        )
    }

    // ── GET /alerts ───────────────────────────────────────────────────────────

    @Test
    fun `GET alerts returns 200 with empty JSON array when no alerts`() = testApp(FakeAlertService()) {
        val response = client.get("/alerts")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `GET alerts returns 200 with alerts list`() = testApp(
        FakeAlertService(mutableListOf(AlertDto(id = "a1", title = "Fire detected")))
    ) {
        val response = client.get("/alerts")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Fire detected"))
    }

    // ── POST /alerts ──────────────────────────────────────────────────────────

    @Test
    fun `POST alerts with valid body returns 201 with id`() = testApp {
        val body = Json.encodeToString(AlertDto(captureId = "cap-1", title = "New alert"))
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("id"))
    }

    // ── GET /alerts/{id} ──────────────────────────────────────────────────────

    @Test
    fun `GET alerts by existing id returns 200 with alert`() = testApp(
        FakeAlertService(mutableListOf(AlertDto(id = "known-id", title = "Test")))
    ) {
        val response = client.get("/alerts/known-id")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("known-id"))
    }

    @Test
    fun `GET alerts by unknown id returns 404`() = testApp(FakeAlertService()) {
        val response = client.get("/alerts/no-such-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── PUT /alerts/{id} ──────────────────────────────────────────────────────

    @Test
    fun `PUT alerts by existing id returns 200`() = testApp(
        FakeAlertService(mutableListOf(AlertDto(id = "known-id", title = "Old")))
    ) {
        val body = Json.encodeToString(AlertDto(title = "Updated", status = "IN_PROGRESS", priority = "HIGH"))
        val response = client.put("/alerts/known-id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT alerts by unknown id returns 404`() = testApp(FakeAlertService()) {
        val body = Json.encodeToString(AlertDto(title = "X", status = "OPEN", priority = "LOW"))
        val response = client.put("/alerts/no-such-id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── DELETE /alerts/{id} ───────────────────────────────────────────────────

    @Test
    fun `DELETE alerts by existing id returns 204`() = testApp(
        FakeAlertService(mutableListOf(AlertDto(id = "known-id")))
    ) {
        val response = client.delete("/alerts/known-id")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE alerts by unknown id returns 404`() = testApp(FakeAlertService()) {
        val response = client.delete("/alerts/no-such-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
