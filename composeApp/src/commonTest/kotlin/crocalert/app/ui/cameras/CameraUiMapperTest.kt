package crocalert.app.ui.cameras

import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CaptureDto
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraUiMapperTest {

    // ── toUiItem(stats) ──────────────────────────────────────────────────────

    @Test
    fun `toUiItem stats - inactive camera is Alert`() {
        val camera = Camera(id = "c1", isActive = false)
        val item = camera.toUiItem(stats(received = 10, expected = 24))
        assertEquals(CameraStatus.Alert, item.status)
    }

    @Test
    fun `toUiItem stats - zero received images is Alert`() {
        val camera = Camera(id = "c1", isActive = true)
        val item = camera.toUiItem(stats(received = 0, expected = 24))
        assertEquals(CameraStatus.Alert, item.status)
    }

    @Test
    fun `toUiItem stats - rate below threshold is Attention`() {
        val camera = Camera(id = "c1", isActive = true)
        val item = camera.toUiItem(stats(received = 20, expected = 24)) // 0.833 < 0.90
        assertEquals(CameraStatus.Attention, item.status)
    }

    @Test
    fun `toUiItem stats - rate at or above threshold is Ok`() {
        val camera = Camera(id = "c1", isActive = true)
        val item = camera.toUiItem(stats(received = 22, expected = 24)) // 0.917 >= 0.90
        assertEquals(CameraStatus.Ok, item.status)
    }

    @Test
    fun `toUiItem stats - camera expectedImages overrides stats expectedImages when positive`() {
        val camera = Camera(id = "c1", isActive = true, expectedImages = 48)
        val item = camera.toUiItem(stats(received = 48, expected = 24))
        assertEquals(48, item.imagesExpected)
    }

    // ── toUiItem(captures) ───────────────────────────────────────────────────

    @Test
    fun `toUiItem captures - empty list and active camera is Alert`() {
        val camera = Camera(id = "c1", isActive = true)
        assertEquals(CameraStatus.Alert, camera.toUiItem(emptyList()).status)
    }

    @Test
    fun `toUiItem captures - rate below threshold is Attention`() {
        val camera = Camera(id = "c1", isActive = true)
        val captures = List(20) { capture(captureTime = withinDayMs()) }
        assertEquals(CameraStatus.Attention, camera.toUiItem(captures, expectedPerDay = 24).status)
    }

    @Test
    fun `toUiItem captures - rate at or above threshold is Ok`() {
        val camera = Camera(id = "c1", isActive = true)
        val captures = List(22) { capture(captureTime = withinDayMs()) }
        assertEquals(CameraStatus.Ok, camera.toUiItem(captures, expectedPerDay = 24).status)
    }

    @Test
    fun `toUiItem captures - integrityFlags counts captures with blank driveUrl`() {
        val camera = Camera(id = "c1", isActive = true)
        val captures = listOf(
            capture(captureTime = withinDayMs(), driveUrl = ""),
            capture(captureTime = withinDayMs(), driveUrl = ""),
            capture(captureTime = withinDayMs(), driveUrl = ""),
            capture(captureTime = withinDayMs(), driveUrl = "https://example.com"),
            capture(captureTime = withinDayMs(), driveUrl = "https://example.com"),
        )
        assertEquals(3, camera.toUiItem(captures, expectedPerDay = 24).integrityFlags)
    }

    // ── toRelativeTime ───────────────────────────────────────────────────────

    @Test
    fun `toRelativeTime - under 1 minute returns Ahora mismo`() {
        val ts = Clock.System.now().toEpochMilliseconds() - 30_000L
        assertEquals("Ahora mismo", ts.toRelativeTime())
    }

    @Test
    fun `toRelativeTime - between 1 and 60 minutes returns Hace N min`() {
        val ts = Clock.System.now().toEpochMilliseconds() - 5 * 60_000L
        assertEquals("Hace 5 min", ts.toRelativeTime())
    }

    @Test
    fun `toRelativeTime - between 1 and 24 hours returns Hace N h`() {
        val ts = Clock.System.now().toEpochMilliseconds() - 3 * 3_600_000L
        assertEquals("Hace 3 h", ts.toRelativeTime())
    }

    @Test
    fun `toRelativeTime - more than 24 hours returns Hace N dias`() {
        val ts = Clock.System.now().toEpochMilliseconds() - 2 * 86_400_000L
        assertEquals("Hace 2 días", ts.toRelativeTime())
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun withinDayMs() = Clock.System.now().toEpochMilliseconds() - 3_600_000L

    private fun stats(received: Int, expected: Int, missing: Int = 0) = CameraDailyStatsDto(
        cameraId = "c1",
        date = "2026-04-23",
        expectedImages = expected,
        receivedImages = received,
        missingImages = missing,
        isActive = true,
    )

    private fun capture(captureTime: Long?, driveUrl: String = "https://example.com") = CaptureDto(
        captureTime = captureTime,
        driveUrl = driveUrl,
    )
}
