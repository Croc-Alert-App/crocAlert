package crocalert.app

import crocalert.server.service.AlertService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

/**
 * Integration tests for the Ktor alert routes.
 *
 * IMPORTANT — Testability limitation:
 * [AlertService] is a concrete class with non-open methods and a lazy Firebase
 * dependency. To unlock the CRUD tests below (currently @Ignored), do ONE of:
 *
 *   Option A (recommended): Extract an `AlertServicePort` interface and make
 *   `configureRouting` accept it instead of the concrete class. Then create a
 *   `FakeAlertService : AlertServicePort` here.
 *
 *   Option B: Mark `AlertService` methods as `open` so they can be overridden.
 *
 * Until then, only the health-check and routing-structure tests run.
 */
class AlertsRouteTest {

    // ── Test application builder ───────────────────────────────────────────────

    private fun testApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            configureSerialization()
            // No configureAuth() — CROC_API_KEY env var will be blank in CI
            configureRouting(AlertService())
        }
        block()
    }

    // ── Health check (no Firebase dependency) ─────────────────────────────────

    @Test
    fun `GET slash returns 200 Server running`() = testApp {
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Server running", response.bodyAsText())
    }

    // ── Routing structure — parameter validation ───────────────────────────────
    // These tests hit routes without calling Firebase (lazy col is never accessed
    // when the path parameter is missing and the handler short-circuits to 400).

    @Test
    fun `GET alerts slash empty id returns 400 BadRequest`() = testApp {
        // Calling /alerts/ with a trailing slash maps to "{id}" = "" which triggers BadRequest
        val response = client.get("/alerts/")
        // Router may return 400 or 404 depending on how Ktor matches the empty segment
        assertTrue(
            response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.NotFound,
            "Expected 400 or 404 but got ${response.status}"
        )
    }

    // ── CRUD route stubs — activate after AlertService refactor ───────────────
    // Remove @Ignore and implement FakeAlertService once AlertServicePort exists.

    @Ignore
    @Test
    fun `GET alerts returns 200 with JSON array`() = testApp {
        // TODO: replace AlertService() with FakeAlertService(alerts = emptyList())
        val response = client.get("/alerts")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().startsWith("["))
    }

    @Ignore
    @Test
    fun `POST alerts with valid body returns 201 with id`() = testApp {
        // TODO: replace AlertService() with FakeAlertService()
        val response = client.post("/alerts") {
            contentType(ContentType.Application.Json)
            setBody("""{"captureId":"cap-1","title":"Test alert","status":"OPEN","priority":"HIGH"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("id"))
    }

    @Ignore
    @Test
    fun `GET alerts by existing id returns 200 with alert JSON`() = testApp {
        // TODO: seed fake service with a known alert
        val response = client.get("/alerts/known-id")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Ignore
    @Test
    fun `GET alerts by unknown id returns 404`() = testApp {
        // TODO: replace AlertService() with FakeAlertService(alerts = emptyList())
        val response = client.get("/alerts/no-such-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Ignore
    @Test
    fun `PUT alerts by existing id returns 200`() = testApp {
        // TODO: seed fake service with a known alert
        val response = client.put("/alerts/known-id") {
            contentType(ContentType.Application.Json)
            setBody("""{"captureId":"cap-1","title":"Updated","status":"IN_PROGRESS","priority":"HIGH"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Ignore
    @Test
    fun `PUT alerts by unknown id returns 404`() = testApp {
        val response = client.put("/alerts/no-such-id") {
            contentType(ContentType.Application.Json)
            setBody("""{"captureId":"cap-1","title":"X","status":"OPEN","priority":"LOW"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Ignore
    @Test
    fun `DELETE alerts by existing id returns 204`() = testApp {
        val response = client.delete("/alerts/known-id")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Ignore
    @Test
    fun `DELETE alerts by unknown id returns 404`() = testApp {
        val response = client.delete("/alerts/no-such-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
