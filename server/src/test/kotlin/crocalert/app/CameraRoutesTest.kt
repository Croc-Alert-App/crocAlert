package crocalert.app

import crocalert.app.shared.data.dto.CameraDto
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraHealthCheckDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto
import crocalert.app.shared.data.dto.HealthStatus
import crocalert.server.service.CameraServicePort
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

private val json = Json { ignoreUnknownKeys = true }

class CameraRoutesTest {

    // ── Fake service (happy path) ─────────────────────────────────────────────

    private class FakeCameraService(
        private val cameras: MutableList<CameraDto> = mutableListOf(),
        private val healthCheck: CameraHealthCheckDto? = defaultHealthCheck(),
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
        override suspend fun getDailyStats(cameraId: String, date: String) = null
        override suspend fun getDailyStatsForAll(date: String) = emptyList<CameraDailyStatsDto>()
        override suspend fun getGlobalDailyCaptureRate(date: String) = GlobalDailyCaptureRateDto(
            date = date, totalCameras = 2, activeCameras = 2,
            expectedImagesTotal = 20, receivedImagesTotal = 18,
            missingImagesTotal = 2, extraImagesTotal = 0, captureRate = 90.0
        )
        override suspend fun getCameraHealthCheck(cameraId: String, date: String) = healthCheck
        override suspend fun getAllCameraHealthChecks(date: String) =
            if (healthCheck != null) listOf(healthCheck) else emptyList()
        override suspend fun getMonitoringDashboard(date: String) = CameraMonitoringDashboardDto(
            date = date, totalCameras = 2, activeCameras = 2,
            expectedImagesTotal = 20, receivedImagesTotal = 18,
            missingImagesTotal = 2, extraImagesTotal = 0, globalCaptureRate = 90.0,
            healthyCameras = 1, cautionCameras = 1, riskCameras = 0,
            healthyRate = 50.0, operationalRate = 100.0,
            cameras = if (healthCheck != null) listOf(healthCheck) else emptyList()
        )

        companion object {
            fun defaultHealthCheck() = CameraHealthCheckDto(
                cameraId = "cam-1", date = "2026-03-17",
                expectedImages = 10, receivedImages = 10, missingImages = 0,
                extraImages = 0, captureRate = 100.0,
                healthStatus = HealthStatus.HEALTHY, isActive = true
            )
        }
    }

    // ── Fake service (throws on every call) ───────────────────────────────────

    private class ThrowingCameraService : CameraServicePort {
        override suspend fun getAll() = throw RuntimeException("Firestore unavailable")
        override suspend fun getById(id: String) = throw RuntimeException("Firestore unavailable")
        override suspend fun create(dto: CameraDto) = throw RuntimeException("Firestore unavailable")
        override suspend fun update(id: String, dto: CameraDto) = throw RuntimeException("Firestore unavailable")
        override suspend fun delete(id: String) = throw RuntimeException("Firestore unavailable")
        override suspend fun getDailyStats(cameraId: String, date: String) = throw RuntimeException("Firestore unavailable")
        override suspend fun getDailyStatsForAll(date: String) = throw RuntimeException("Firestore unavailable")
        override suspend fun getGlobalDailyCaptureRate(date: String) = throw RuntimeException("Firestore unavailable")
        override suspend fun getCameraHealthCheck(cameraId: String, date: String) = throw RuntimeException("Firestore unavailable")
        override suspend fun getAllCameraHealthChecks(date: String) = throw RuntimeException("Firestore unavailable")
        override suspend fun getMonitoringDashboard(date: String) = throw RuntimeException("Firestore unavailable")
    }

    // ── Fake service (empty fleet) ─────────────────────────────────────────────

    private class EmptyFleetCameraService : CameraServicePort {
        override suspend fun getAll() = emptyList<CameraDto>()
        override suspend fun getById(id: String) = null
        override suspend fun create(dto: CameraDto) = "new-id"
        override suspend fun update(id: String, dto: CameraDto) = false
        override suspend fun delete(id: String) = false
        override suspend fun getDailyStats(cameraId: String, date: String) = null
        override suspend fun getDailyStatsForAll(date: String) = emptyList<CameraDailyStatsDto>()
        override suspend fun getGlobalDailyCaptureRate(date: String) = GlobalDailyCaptureRateDto(
            date = date, totalCameras = 0, activeCameras = 0,
            expectedImagesTotal = 0, receivedImagesTotal = 0,
            missingImagesTotal = 0, extraImagesTotal = 0, captureRate = 0.0
        )
        override suspend fun getCameraHealthCheck(cameraId: String, date: String) = null
        override suspend fun getAllCameraHealthChecks(date: String) = emptyList<CameraHealthCheckDto>()
        override suspend fun getMonitoringDashboard(date: String) = CameraMonitoringDashboardDto(
            date = date, totalCameras = 0, activeCameras = 0,
            expectedImagesTotal = 0, receivedImagesTotal = 0,
            missingImagesTotal = 0, extraImagesTotal = 0, globalCaptureRate = 0.0,
            healthyCameras = 0, cautionCameras = 0, riskCameras = 0,
            healthyRate = 0.0, operationalRate = 0.0, cameras = emptyList()
        )
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

    // ── GET /cameras/global-daily-rate/{date} ─────────────────────────────────

    @Test
    fun `GET global-daily-rate with valid date returns 200`() = testApp {
        val response = client.get("/cameras/global-daily-rate/2026-03-17")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("captureRate"))
    }

    @Test
    fun `GET global-daily-rate with invalid date format returns 400`() = testApp {
        val response = client.get("/cameras/global-daily-rate/17-03-2026")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("yyyy-MM-dd"))
    }

    // ── GET /cameras/{id}/health-check/{date} ────────────────────────────────

    @Test
    fun `GET camera health-check with valid date returns 200 with typed fields`() = testApp {
        val response = client.get("/cameras/cam-1/health-check/2026-03-17")
        assertEquals(HttpStatusCode.OK, response.status)
        // P37: decode to DTO and assert on typed field — not brittle string match
        val dto = json.decodeFromString<CameraHealthCheckDto>(response.bodyAsText())
        assertEquals(HealthStatus.HEALTHY, dto.healthStatus)
        assertEquals("cam-1", dto.cameraId)
    }

    @Test
    fun `GET camera health-check for unknown camera returns 404 with camera id`() = testApp(
        FakeCameraService(healthCheck = null)
    ) {
        val response = client.get("/cameras/no-such-cam/health-check/2026-03-17")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("no-such-cam"))
    }

    @Test
    fun `GET camera health-check with invalid date format returns 400`() = testApp {
        val response = client.get("/cameras/cam-1/health-check/2026-3-17")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("yyyy-MM-dd"))
    }

    // ── GET /cameras/health-checks/{date} ────────────────────────────────────

    @Test
    fun `GET all health-checks with valid date returns 200 with typed fields`() = testApp {
        val response = client.get("/cameras/health-checks/2026-03-17")
        assertEquals(HttpStatusCode.OK, response.status)
        // P37: decode to typed list — not brittle string match
        val dtos = json.decodeFromString<List<CameraHealthCheckDto>>(response.bodyAsText())
        assertEquals(1, dtos.size)
        assertEquals(HealthStatus.HEALTHY, dtos.first().healthStatus)
    }

    @Test
    fun `GET all health-checks returns empty list for empty fleet`() = testApp(EmptyFleetCameraService()) {
        val response = client.get("/cameras/health-checks/2026-03-17")
        assertEquals(HttpStatusCode.OK, response.status)
        val dtos = json.decodeFromString<List<CameraHealthCheckDto>>(response.bodyAsText())
        assertTrue(dtos.isEmpty())
    }

    @Test
    fun `GET all health-checks with invalid date format returns 400`() = testApp {
        val response = client.get("/cameras/health-checks/20260317")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("yyyy-MM-dd"))
    }

    // ── GET /cameras/dashboard/{date} ─────────────────────────────────────────

    @Test
    fun `GET dashboard with valid date returns 200`() = testApp {
        val response = client.get("/cameras/dashboard/2026-03-17")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("globalCaptureRate"))
    }

    @Test
    fun `GET dashboard with invalid date format returns 400`() = testApp {
        val response = client.get("/cameras/dashboard/today")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("yyyy-MM-dd"))
    }

    @Test
    fun `GET dashboard with empty fleet returns 200 with zero counts`() = testApp(EmptyFleetCameraService()) {
        val response = client.get("/cameras/dashboard/2026-03-17")
        assertEquals(HttpStatusCode.OK, response.status)
        val dto = json.decodeFromString<CameraMonitoringDashboardDto>(response.bodyAsText())
        assertEquals(0, dto.totalCameras)
        assertEquals(0, dto.healthyCameras)
    }

    // ── P38: impossible calendar dates rejected ────────────────────────────────

    @Test
    fun `GET dashboard rejects impossible date 2026-13-45`() = testApp {
        val response = client.get("/cameras/dashboard/2026-13-45")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET health-checks rejects February 30`() = testApp {
        val response = client.get("/cameras/health-checks/2026-02-30")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // ── P38: service failure returns 500 with structured error ─────────────────

    @Test
    fun `GET dashboard returns 500 when service throws`() = testApp(ThrowingCameraService()) {
        val response = client.get("/cameras/dashboard/2026-03-17")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(response.bodyAsText().contains("error"))
    }

    @Test
    fun `GET all health-checks returns 500 when service throws`() = testApp(ThrowingCameraService()) {
        val response = client.get("/cameras/health-checks/2026-03-17")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(response.bodyAsText().contains("error"))
    }

    @Test
    fun `GET global-daily-rate returns 500 when service throws`() = testApp(ThrowingCameraService()) {
        val response = client.get("/cameras/global-daily-rate/2026-03-17")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }
}
