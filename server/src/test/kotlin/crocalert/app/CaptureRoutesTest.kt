package crocalert.app

import crocalert.app.shared.data.dto.CaptureDto
import crocalert.server.service.CaptureServicePort
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class CaptureRoutesTest {

    // ── Fake service ──────────────────────────────────────────────────────────

    private class FakeCaptureService(
        private val captures: MutableList<CaptureDto> = mutableListOf()
    ) : CaptureServicePort {
        override suspend fun getAll() = captures.toList()
        override suspend fun getById(id: String) = captures.firstOrNull { it.id == id }
        override suspend fun getByCameraId(cameraId: String) =
            captures.filter { it.cameraId == cameraId }
        override suspend fun getByFolder(folder: String) =
            captures.filter { it.folder == folder }
        override suspend fun create(dto: CaptureDto): String {
            val id = dto.id.ifBlank { "generated-id" }
            captures += dto.copy(id = id)
            return id
        }
        override suspend fun update(id: String, dto: CaptureDto): Boolean {
            val idx = captures.indexOfFirst { it.id == id }
            if (idx == -1) return false
            captures[idx] = dto.copy(id = id)
            return true
        }
        override suspend fun delete(id: String): Boolean = captures.removeIf { it.id == id }
    }

    // ── Test application builder ───────────────────────────────────────────────

    private fun testApp(
        service: CaptureServicePort = FakeCaptureService(),
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        application {
            configureSerialization()
            configureRouting(captureService = service)
        }
        block()
    }

    // ── GET /captures ─────────────────────────────────────────────────────────

    @Test
    fun `GET captures returns 200 with empty list`() = testApp(FakeCaptureService()) {
        val response = client.get("/captures")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `GET captures returns 200 with capture list`() = testApp(
        FakeCaptureService(mutableListOf(CaptureDto(id = "cap-1", name = "img001.jpg")))
    ) {
        val response = client.get("/captures")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("img001.jpg"))
    }

    // ── POST /captures ────────────────────────────────────────────────────────

    @Test
    fun `POST captures with valid body returns 201 with id`() = testApp {
        val body = Json.encodeToString(CaptureDto(cameraId = "cam-1", name = "img.jpg", driveId = "drv-1", driveUrl = "http://drive/img"))
        val response = client.post("/captures") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("id"))
    }

    // ── GET /captures/{id} ────────────────────────────────────────────────────

    @Test
    fun `GET captures by existing id returns 200`() = testApp(
        FakeCaptureService(mutableListOf(CaptureDto(id = "cap-1", name = "img.jpg")))
    ) {
        val response = client.get("/captures/cap-1")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("cap-1"))
    }

    @Test
    fun `GET captures by unknown id returns 404`() = testApp(FakeCaptureService()) {
        val response = client.get("/captures/no-such-cap")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── GET /captures/by-camera/{cameraId} ───────────────────────────────────

    @Test
    fun `GET captures by-camera returns filtered list`() = testApp(
        FakeCaptureService(mutableListOf(
            CaptureDto(id = "cap-1", cameraId = "cam-A", name = "a.jpg"),
            CaptureDto(id = "cap-2", cameraId = "cam-B", name = "b.jpg")
        ))
    ) {
        val response = client.get("/captures/by-camera/cam-A")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("a.jpg"))
        assertFalse(body.contains("b.jpg"))
    }

    @Test
    fun `GET captures by-camera returns empty list for unknown camera`() = testApp(FakeCaptureService()) {
        val response = client.get("/captures/by-camera/no-cam")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    // ── PUT /captures/{id} ────────────────────────────────────────────────────

    @Test
    fun `PUT captures by existing id returns 200`() = testApp(
        FakeCaptureService(mutableListOf(CaptureDto(id = "cap-1", name = "old.jpg")))
    ) {
        val body = Json.encodeToString(CaptureDto(name = "new.jpg", driveId = "d", driveUrl = "u"))
        val response = client.put("/captures/cap-1") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT captures by unknown id returns 404`() = testApp(FakeCaptureService()) {
        val body = Json.encodeToString(CaptureDto(name = "x.jpg"))
        val response = client.put("/captures/no-such-cap") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── DELETE /captures/{id} ─────────────────────────────────────────────────

    @Test
    fun `DELETE captures by existing id returns 204`() = testApp(
        FakeCaptureService(mutableListOf(CaptureDto(id = "cap-1")))
    ) {
        val response = client.delete("/captures/cap-1")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE captures by unknown id returns 404`() = testApp(FakeCaptureService()) {
        val response = client.delete("/captures/no-such-cap")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
