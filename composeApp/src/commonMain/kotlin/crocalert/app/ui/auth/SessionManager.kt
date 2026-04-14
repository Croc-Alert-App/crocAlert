package crocalert.app.ui.auth

import kotlinx.datetime.Clock

private const val SESSION_DURATION_MS = 72 * 60 * 60 * 1000L // 72 hours

/**
 * Describes the session state detected at app launch.
 *
 * Derive this via [SessionManager.checkSession] — a single DataStore read guarantees
 * a consistent result even if another coroutine modifies prefs concurrently.
 */
enum class SessionCheckResult { Active, Expired, None }

object SessionManager {

    private var prefs: SessionPreferences? = null

    /** Call once at app startup (before the Compose tree) with a platform-specific store. */
    fun init(preferences: SessionPreferences) {
        prefs = preferences
    }

    /**
     * Checks session state in a single atomic read and returns the result.
     *
     * This replaces the previous pair of [isSessionValid] + [isSessionExpired] calls,
     * which were prone to a TOCTOU race: two separate DataStore reads could produce
     * an inconsistent snapshot if another coroutine wrote prefs in between.
     *
     * Result semantics:
     * - [SessionCheckResult.Active]  — session exists and has not expired.
     * - [SessionCheckResult.Expired] — session existed but the expiry timestamp is in the past.
     * - [SessionCheckResult.None]    — no session stored (first launch or after logout).
     */
    suspend fun checkSession(): SessionCheckResult {
        val expiresAt = prefs?.getSessionExpiresAt() ?: return SessionCheckResult.None
        return if (Clock.System.now().toEpochMilliseconds() < expiresAt) {
            SessionCheckResult.Active
        } else {
            SessionCheckResult.Expired
        }
    }

    /** Returns the email saved by a previous "Recordar dispositivo" login, or null. */
    suspend fun getSavedEmail(): String? = prefs?.getSavedEmail()

    /**
     * Atomically updates the remembered-device state after a successful authentication.
     *
     * - [remember] = true  -> persists [email] and starts a session window.
     * - [remember] = false -> clears both the saved email and any active session.
     *
     * Both values are written in a single DataStore transaction to prevent inconsistent
     * state if the process is killed between two separate writes.
     */
    suspend fun updateRememberDevice(email: String, remember: Boolean) {
        if (remember) {
            val expiresAt = Clock.System.now().toEpochMilliseconds() + SESSION_DURATION_MS
            prefs?.updateSession(email = email, expiresAt = expiresAt)
        } else {
            prefs?.updateSession(email = null, expiresAt = null)
        }
    }

    /**
     * Ends the active session on explicit logout.
     *
     * Both the session expiry and the saved email are cleared. The email is not
     * preserved after logout to prevent a previous user's email from being pre-filled
     * when a different user logs in on the same device.
     */
    suspend fun logout() {
        prefs?.updateSession(email = null, expiresAt = null)
        crocalert.app.shared.UserSession.clear()
    }

    /**
     * Returns the milliseconds remaining until the session expires, or null if no session
     * is stored. A value of 0 means the session is already expired.
     */
    suspend fun sessionRemainingMs(): Long? {
        val expiresAt = prefs?.getSessionExpiresAt() ?: return null
        return maxOf(0L, expiresAt - Clock.System.now().toEpochMilliseconds())
    }
}
