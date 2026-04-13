package crocalert.app.ui.auth

import kotlinx.datetime.Clock

private const val SESSION_DURATION_MS =  72 * 60 * 60 * 1000L // 72 hours

object SessionManager {

    private var prefs: SessionPreferences? = null

    /** Call once at app startup (before the Compose tree) with a platform-specific store. */
    fun init(preferences: SessionPreferences) {
        prefs = preferences
    }

    /**
     * Returns true if a remembered session exists and has not expired.
     * Used by SplashScreen to decide whether to skip the login screen.
     */
    suspend fun isSessionValid(): Boolean {
        val expiresAt = prefs?.getSessionExpiresAt() ?: return false
        return Clock.System.now().toEpochMilliseconds() < expiresAt
    }

    /**
     * Returns true if a session was previously stored but has now expired.
     * Used to show an "expired session" notice on the login screen.
     */
    suspend fun isSessionExpired(): Boolean {
        val expiresAt = prefs?.getSessionExpiresAt() ?: return false
        return Clock.System.now().toEpochMilliseconds() >= expiresAt
    }

    /** Returns the email saved by a previous "Recordar dispositivo" login, or null. */
    suspend fun getSavedEmail(): String? = prefs?.getSavedEmail()

    /**
     * Updates the remembered-device state after a successful authentication.
     *
     * - [remember] = true  -> persists [email] and starts a session window.
     * - [remember] = false -> clears both the saved email and any active session.
     */
    suspend fun updateRememberDevice(email: String, remember: Boolean) {
        if (remember) {
            prefs?.setSavedEmail(email)
            prefs?.setSessionExpiresAt(Clock.System.now().toEpochMilliseconds() + SESSION_DURATION_MS)
        } else {
            prefs?.setSavedEmail(null)
            prefs?.setSessionExpiresAt(null)
        }
    }

    /**
     * Ends the active session on explicit logout.
     * The session expiry is cleared (next launch will require login) but the
     * saved email is preserved so the login form can pre-fill it.
     */
    suspend fun logout() {
        prefs?.setSessionExpiresAt(null)
        crocalert.app.shared.UserSession.clear()
    }

    /**
     * Returns the milliseconds remaining until the session expires, or null if no session is stored.
     * A value of 0 means the session is already expired.
     */
    suspend fun sessionRemainingMs(): Long? {
        val expiresAt = prefs?.getSessionExpiresAt() ?: return null
        return maxOf(0L, expiresAt - Clock.System.now().toEpochMilliseconds())
    }

    /** Temporary debug helper — remove before release. */
    suspend fun debugInfo(): String {
        val expiresAt = prefs?.getSessionExpiresAt()
        val email = prefs?.getSavedEmail()
        val now = Clock.System.now().toEpochMilliseconds()
        return if (expiresAt == null) {
            "No session. Email='$email'"
        } else {
            val remainingMs = expiresAt - now
            "Email='$email' | Remaining: ${remainingMs}ms | Valid: ${remainingMs > 0}"
        }
    }
}
