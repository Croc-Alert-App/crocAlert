package crocalert.app.domain

import crocalert.app.model.AlertStatus

/**
 * Enforces valid status transitions for the Alert lifecycle (R-01).
 *
 * Allowed transitions:
 *   OPEN         → IN_PROGRESS  (operator picks up the alert)
 *   OPEN         → CLOSED       (dismissed without action)
 *   IN_PROGRESS  → CLOSED       (resolved)
 *
 * Disallowed:
 *   CLOSED → any  (terminal state — re-opening requires creating a new alert)
 *   IN_PROGRESS → OPEN  (no backward movement)
 *   same → same         (not a transition)
 */
object AlertStatusValidator {

    private val allowedTransitions: Map<AlertStatus, Set<AlertStatus>> = mapOf(
        AlertStatus.OPEN        to setOf(AlertStatus.IN_PROGRESS, AlertStatus.CLOSED),
        AlertStatus.IN_PROGRESS to setOf(AlertStatus.CLOSED),
        AlertStatus.CLOSED      to emptySet(),
    )

    /**
     * Returns `true` if transitioning from [from] to [to] is permitted by the state machine.
     * Same-to-same is always `false` — use an explicit equality check for no-op detection.
     */
    fun isValidTransition(from: AlertStatus, to: AlertStatus): Boolean {
        if (from == to) return false
        return allowedTransitions[from]?.contains(to) ?: false
    }

    /**
     * Throws [IllegalStateException] when the transition is not permitted.
     * Use this as a domain guard before persisting a status change.
     */
    fun requireValidTransition(from: AlertStatus, to: AlertStatus) {
        check(isValidTransition(from, to)) {
            "Invalid alert status transition: $from → $to. " +
                "Allowed from $from: ${allowedTransitions[from]?.joinToString() ?: "none"}"
        }
    }

    /** Returns all valid next statuses from [current], or an empty set if terminal. */
    fun nextStates(current: AlertStatus): Set<AlertStatus> =
        allowedTransitions[current] ?: emptySet()

    /** Returns `true` if [status] is a terminal state (no further transitions possible). */
    fun isTerminal(status: AlertStatus): Boolean = nextStates(status).isEmpty()
}
