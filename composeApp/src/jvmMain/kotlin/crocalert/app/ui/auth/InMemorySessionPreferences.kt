package crocalert.app.ui.auth

class InMemorySessionPreferences : SessionPreferences {
    private var savedEmail: String? = null
    private var sessionExpiresAt: Long? = null

    override suspend fun getSavedEmail(): String? = savedEmail
    override suspend fun setSavedEmail(email: String?) { savedEmail = email }

    override suspend fun getSessionExpiresAt(): Long? = sessionExpiresAt
    override suspend fun setSessionExpiresAt(expiresAt: Long?) { sessionExpiresAt = expiresAt }
}
