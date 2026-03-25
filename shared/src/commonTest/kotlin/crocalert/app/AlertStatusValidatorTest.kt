package crocalert.app

import crocalert.app.domain.AlertStatusValidator
import crocalert.app.model.AlertStatus
import kotlin.test.*

/**
 * Tests for [AlertStatusValidator] (R-01).
 *
 * Verifies every cell of the state machine transition table and the helper
 * methods used by the domain layer before persisting status changes.
 */
@UnitTest
class AlertStatusValidatorTest {

    // ── isValidTransition — allowed paths ──────────────────────────────────────

    @Test
    fun `OPEN to IN_PROGRESS is allowed`() {
        assertTrue(AlertStatusValidator.isValidTransition(AlertStatus.OPEN, AlertStatus.IN_PROGRESS))
    }

    @Test
    fun `OPEN to CLOSED is allowed (dismiss without action)`() {
        assertTrue(AlertStatusValidator.isValidTransition(AlertStatus.OPEN, AlertStatus.CLOSED))
    }

    @Test
    fun `IN_PROGRESS to CLOSED is allowed (resolved)`() {
        assertTrue(AlertStatusValidator.isValidTransition(AlertStatus.IN_PROGRESS, AlertStatus.CLOSED))
    }

    // ── isValidTransition — disallowed paths ──────────────────────────────────

    @Test
    fun `CLOSED to OPEN is disallowed (terminal state)`() {
        assertFalse(AlertStatusValidator.isValidTransition(AlertStatus.CLOSED, AlertStatus.OPEN))
    }

    @Test
    fun `CLOSED to IN_PROGRESS is disallowed (terminal state)`() {
        assertFalse(AlertStatusValidator.isValidTransition(AlertStatus.CLOSED, AlertStatus.IN_PROGRESS))
    }

    @Test
    fun `IN_PROGRESS to OPEN is disallowed (no backward movement)`() {
        assertFalse(AlertStatusValidator.isValidTransition(AlertStatus.IN_PROGRESS, AlertStatus.OPEN))
    }

    // ── isValidTransition — same-to-same ──────────────────────────────────────

    @Test
    fun `OPEN to OPEN is not a transition`() {
        assertFalse(AlertStatusValidator.isValidTransition(AlertStatus.OPEN, AlertStatus.OPEN))
    }

    @Test
    fun `IN_PROGRESS to IN_PROGRESS is not a transition`() {
        assertFalse(AlertStatusValidator.isValidTransition(AlertStatus.IN_PROGRESS, AlertStatus.IN_PROGRESS))
    }

    @Test
    fun `CLOSED to CLOSED is not a transition`() {
        assertFalse(AlertStatusValidator.isValidTransition(AlertStatus.CLOSED, AlertStatus.CLOSED))
    }

    // ── requireValidTransition — success paths ────────────────────────────────

    @Test
    fun `requireValidTransition does not throw for OPEN to IN_PROGRESS`() {
        AlertStatusValidator.requireValidTransition(AlertStatus.OPEN, AlertStatus.IN_PROGRESS)
        // No exception = pass
    }

    @Test
    fun `requireValidTransition does not throw for OPEN to CLOSED`() {
        AlertStatusValidator.requireValidTransition(AlertStatus.OPEN, AlertStatus.CLOSED)
    }

    @Test
    fun `requireValidTransition does not throw for IN_PROGRESS to CLOSED`() {
        AlertStatusValidator.requireValidTransition(AlertStatus.IN_PROGRESS, AlertStatus.CLOSED)
    }

    // ── requireValidTransition — throws on invalid ────────────────────────────

    @Test
    fun `requireValidTransition throws for CLOSED to OPEN`() {
        val ex = assertFailsWith<IllegalStateException> {
            AlertStatusValidator.requireValidTransition(AlertStatus.CLOSED, AlertStatus.OPEN)
        }
        assertTrue(ex.message!!.contains("CLOSED"), "Message should mention origin status")
        assertTrue(ex.message!!.contains("OPEN"), "Message should mention target status")
    }

    @Test
    fun `requireValidTransition throws for IN_PROGRESS to OPEN`() {
        assertFailsWith<IllegalStateException> {
            AlertStatusValidator.requireValidTransition(AlertStatus.IN_PROGRESS, AlertStatus.OPEN)
        }
    }

    @Test
    fun `requireValidTransition throws for same-to-same (OPEN)`() {
        assertFailsWith<IllegalStateException> {
            AlertStatusValidator.requireValidTransition(AlertStatus.OPEN, AlertStatus.OPEN)
        }
    }

    @Test
    fun `requireValidTransition throws for same-to-same (CLOSED)`() {
        assertFailsWith<IllegalStateException> {
            AlertStatusValidator.requireValidTransition(AlertStatus.CLOSED, AlertStatus.CLOSED)
        }
    }

    // ── nextStates ────────────────────────────────────────────────────────────

    @Test
    fun `nextStates for OPEN returns IN_PROGRESS and CLOSED`() {
        val next = AlertStatusValidator.nextStates(AlertStatus.OPEN)
        assertEquals(setOf(AlertStatus.IN_PROGRESS, AlertStatus.CLOSED), next)
    }

    @Test
    fun `nextStates for IN_PROGRESS returns only CLOSED`() {
        val next = AlertStatusValidator.nextStates(AlertStatus.IN_PROGRESS)
        assertEquals(setOf(AlertStatus.CLOSED), next)
    }

    @Test
    fun `nextStates for CLOSED returns empty set`() {
        val next = AlertStatusValidator.nextStates(AlertStatus.CLOSED)
        assertTrue(next.isEmpty())
    }

    // ── isTerminal ────────────────────────────────────────────────────────────

    @Test
    fun `CLOSED is terminal`() {
        assertTrue(AlertStatusValidator.isTerminal(AlertStatus.CLOSED))
    }

    @Test
    fun `OPEN is not terminal`() {
        assertFalse(AlertStatusValidator.isTerminal(AlertStatus.OPEN))
    }

    @Test
    fun `IN_PROGRESS is not terminal`() {
        assertFalse(AlertStatusValidator.isTerminal(AlertStatus.IN_PROGRESS))
    }

    // ── Full lifecycle walk-through ───────────────────────────────────────────

    @Test
    fun `full lifecycle OPEN to IN_PROGRESS to CLOSED is all valid`() {
        // Each step should be valid
        assertTrue(AlertStatusValidator.isValidTransition(AlertStatus.OPEN, AlertStatus.IN_PROGRESS))
        assertTrue(AlertStatusValidator.isValidTransition(AlertStatus.IN_PROGRESS, AlertStatus.CLOSED))
        // Terminal at end
        assertTrue(AlertStatusValidator.isTerminal(AlertStatus.CLOSED))
    }

    @Test
    fun `shortcut lifecycle OPEN to CLOSED is valid`() {
        assertTrue(AlertStatusValidator.isValidTransition(AlertStatus.OPEN, AlertStatus.CLOSED))
        assertTrue(AlertStatusValidator.isTerminal(AlertStatus.CLOSED))
    }
}
