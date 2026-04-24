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

    /**
     * Atomically persists both [email] and [expiresAt] in a single write operation.
     * Pass null for either field to clear it. Implementations must guarantee that
     * both values are written together so a mid-write kill cannot leave them inconsistent.
     */
    suspend fun updateSession(email: String?, expiresAt: Long?)
}
