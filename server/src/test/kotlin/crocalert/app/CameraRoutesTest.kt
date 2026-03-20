package crocalert.app

import crocalert.app.shared.data.dto.CameraDto
import crocalert.server.service.CameraServicePort
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class CameraRoutesTest {

    // ── Fake service ──────────────────────────────────────────────────────────

    private class FakeCameraService(
        private val cameras: MutableList<CameraDto> = mutableListOf()
    ) : CameraServicePort {
        override suspend fun getAll() = cameras.toList()
        override suspend fun getById(id: String) = cameras.firstOrNull { it.id == id }
        override suspend fun create(dto: CameraDto): String {
            val id = dto.id.ifBlank { "generated-id" }
            cameras += dto.copy(id = id)
            return id
        }
        override suspend fun update(id: String, dto: CameraDto): Boolean {
            val idx = cameras.indexOfFirst { it.id == id }
            if (idx == -1) return false
            cameras[idx] = dto.copy(id = id)
            return true
        }
        override suspend fun delete(id: String): Boolean = cameras.removeIf { it.id == id }
    }

    // ── Test application builder ───────────────────────────────────────────────

    private fun testApp(
        service: CameraServicePort = FakeCameraService(),
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        application {
            configureSerialization()
            configureRouting(cameraService = service)
        }
        block()
    }

    // ── GET /cameras ──────────────────────────────────────────────────────────

    @Test
    fun `GET cameras returns 200 with empty list`() = testApp(FakeCameraService()) {
        val response = client.get("/cameras")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `GET cameras returns 200 with camera list`() = testApp(
        FakeCameraService(mutableListOf(CameraDto(id = "cam-1", name = "Entrance")))
    ) {
        val response = client.get("/cameras")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Entrance"))
    }

    // ── POST /cameras ─────────────────────────────────────────────────────────

    @Test
    fun `POST cameras with valid body returns 201 with id`() = testApp {
        val body = Json.encodeToString(CameraDto(name = "New cam", isActive = true))
        val response = client.post("/cameras") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("id"))
    }

    // ── GET /cameras/{id} ─────────────────────────────────────────────────────

    @Test
    fun `GET cameras by existing id returns 200`() = testApp(
        FakeCameraService(mutableListOf(CameraDto(id = "cam-1", name = "Entrance")))
    ) {
        val response = client.get("/cameras/cam-1")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("cam-1"))
    }

    @Test
    fun `GET cameras by unknown id returns 404`() = testApp(FakeCameraService()) {
        val response = client.get("/cameras/no-such-cam")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── PUT /cameras/{id} ─────────────────────────────────────────────────────

    @Test
    fun `PUT cameras by existing id returns 200`() = testApp(
        FakeCameraService(mutableListOf(CameraDto(id = "cam-1", name = "Old name")))
    ) {
        val body = Json.encodeToString(CameraDto(name = "New name", isActive = false))
        val response = client.put("/cameras/cam-1") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT cameras by unknown id returns 404`() = testApp(FakeCameraService()) {
        val body = Json.encodeToString(CameraDto(name = "X"))
        val response = client.put("/cameras/no-such-cam") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── DELETE /cameras/{id} ──────────────────────────────────────────────────

    @Test
    fun `DELETE cameras by existing id returns 204`() = testApp(
        FakeCameraService(mutableListOf(CameraDto(id = "cam-1")))
    ) {
        val response = client.delete("/cameras/cam-1")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE cameras by unknown id returns 404`() = testApp(FakeCameraService()) {
        val response = client.delete("/cameras/no-such-cam")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
