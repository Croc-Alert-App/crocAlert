package crocalert.app.ui.auth

sealed class AuthSignInResult {
    data object Success : AuthSignInResult()
    data object MfaRequired : AuthSignInResult()
    data object MfaEnrollmentRequired : AuthSignInResult()
    data object EmailNotVerified : AuthSignInResult()
    data class Error(val message: String) : AuthSignInResult()
}

sealed class TotpSetupResult {
    data class Success(val uri: String, val secretKey: String) : TotpSetupResult()
    data class Error(val message: String) : TotpSetupResult()
}

expect object FirebaseAuthClient {
    suspend fun signIn(email: String, password: String): AuthSignInResult
    suspend fun verifyTotp(otp: String): AuthSignInResult
    suspend fun register(email: String, password: String, fullName: String, role: String): AuthSignInResult
    suspend fun sendVerificationEmail(): AuthSignInResult
    suspend fun generateTotpSetup(): TotpSetupResult
    suspend fun enrollTotp(otp: String): AuthSignInResult

    /**
     * Sends a password reset email to [email].
     * Succeeds silently even if no account exists (privacy — callers should not
     * reveal whether the address is registered).
     */
    suspend fun sendPasswordReset(email: String): AuthSignInResult

    /**
     * Reads the currently signed-in Firebase user and populates [UserSession].
     * Called on app startup when a remembered session is detected, so the user
     * data is available without going through the login flow again.
     * Returns true if a Firebase user was found and the session was restored.
     */
    suspend fun restoreSession(): Boolean
}
