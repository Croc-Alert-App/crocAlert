package crocalert.app

import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.data.mapper.toDto
import crocalert.app.shared.data.mapper.toModel
import kotlin.test.*

/**
 * Tests for AlertDto ↔ Alert mapping (R-12).
 *
 * The server may return an enum string the client doesn't recognise after a
 * backend expansion. enumFromStringOrDefault must degrade gracefully rather than throw.
 */
@crocalert.app.UnitTest
class AlertMapperTest {

    // ── Unknown status string degrades to OPEN ────────────────────────────────

    @Test
    fun `unknown status string maps to OPEN without throwing`() {
        val alert = AlertDto(status = "EXPIRED").toModel()
        assertEquals(AlertStatus.OPEN, alert.status)
    }

    @Test
    fun `empty status string maps to OPEN without throwing`() {
        val alert = AlertDto(status = "").toModel()
        assertEquals(AlertStatus.OPEN, alert.status)
    }

    @Test
    fun `mixed-case status string maps to OPEN (case-sensitive guard)`() {
        val alert = AlertDto(status = "open").toModel()
        assertEquals(AlertStatus.OPEN, alert.status)
    }

    // ── Unknown priority string degrades to MEDIUM ────────────────────────────

    @Test
    fun `unknown priority string maps to MEDIUM without throwing`() {
        val alert = AlertDto(priority = "URGENT").toModel()
        assertEquals(AlertPriority.MEDIUM, alert.priority)
    }

    @Test
    fun `empty priority string maps to MEDIUM without throwing`() {
        val alert = AlertDto(priority = "").toModel()
        assertEquals(AlertPriority.MEDIUM, alert.priority)
    }

    // ── All known status values round-trip correctly ───────────────────────────

    @Test
    fun `OPEN status round-trips through DTO`() {
        val dto = AlertDto(status = "OPEN")
        assertEquals(AlertStatus.OPEN, dto.toModel().status)
    }

    @Test
    fun `IN_PROGRESS status round-trips through DTO`() {
        val dto = AlertDto(status = "IN_PROGRESS")
        assertEquals(AlertStatus.IN_PROGRESS, dto.toModel().status)
    }

    @Test
    fun `CLOSED status round-trips through DTO`() {
        val dto = AlertDto(status = "CLOSED")
        assertEquals(AlertStatus.CLOSED, dto.toModel().status)
    }

    // ── All known priority values round-trip correctly ────────────────────────

    @Test
    fun `LOW priority round-trips through DTO`() {
        assertEquals(AlertPriority.LOW, AlertDto(priority = "LOW").toModel().priority)
    }

    @Test
    fun `MEDIUM priority round-trips through DTO`() {
        assertEquals(AlertPriority.MEDIUM, AlertDto(priority = "MEDIUM").toModel().priority)
    }

    @Test
    fun `HIGH priority round-trips through DTO`() {
        assertEquals(AlertPriority.HIGH, AlertDto(priority = "HIGH").toModel().priority)
    }

    @Test
    fun `CRITICAL priority round-trips through DTO`() {
        assertEquals(AlertPriority.CRITICAL, AlertDto(priority = "CRITICAL").toModel().priority)
    }

    // ── toDto preserves enum names for server serialization ───────────────────

    @Test
    fun `toDto encodes status as its enum name`() {
        val model = AlertDto(status = "IN_PROGRESS").toModel()
        assertEquals("IN_PROGRESS", model.toDto().status)
    }

    @Test
    fun `toDto encodes priority as its enum name`() {
        val model = AlertDto(priority = "CRITICAL").toModel()
        assertEquals("CRITICAL", model.toDto().priority)
    }

    // ── Scalar fields pass through unchanged ──────────────────────────────────

    @Test
    fun `toModel preserves all scalar fields`() {
        val dto = AlertDto(
            id = "x1",
            captureId = "cap-99",
            createdAt = 5_000L,
            assignedToUserId = "user-7",
            closedAt = 9_000L,
            notes = "Test note",
            title = "Fire alarm",
        )
        val model = dto.toModel()
        assertEquals("x1", model.id)
        assertEquals("cap-99", model.captureId)
        assertEquals(5_000L, model.createdAt)
        assertEquals("user-7", model.assignedToUserId)
        assertEquals(9_000L, model.closedAt)
        assertEquals("Test note", model.notes)
        assertEquals("Fire alarm", model.title)
    }
}
