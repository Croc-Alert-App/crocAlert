package crocalert.app.ui.auth

interface SessionPreferences {
    /** Returns the saved email address, or null if none is stored. */
    suspend fun getSavedEmail(): String?

    /** Persists [email] for pre-filling the login form, or clears it when null. */
    suspend fun setSavedEmail(email: String?)

    /**
     * Returns the epoch-millisecond timestamp at which the active session expires,
     * or null if no session is stored.
     */
    suspend fun getSessionExpiresAt(): Long?

    /** Stores the expiry timestamp, or clears it when null. */
    suspend fun setSessionExpiresAt(expiresAt: Long?)
}
