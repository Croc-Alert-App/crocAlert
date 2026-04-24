package crocalert.app.ui.auth

class InMemorySessionPreferences : SessionPreferences {
    private var email: String? = null
    private var expiresAt: Long? = null

    override suspend fun getSavedEmail(): String? = email
    override suspend fun setSavedEmail(email: String?) { this.email = email }
    override suspend fun getSessionExpiresAt(): Long? = expiresAt
    override suspend fun setSessionExpiresAt(expiresAt: Long?) { this.expiresAt = expiresAt }
    override suspend fun updateSession(email: String?, expiresAt: Long?) {
        this.email = email
        this.expiresAt = expiresAt
    }
}
