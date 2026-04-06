package crocalert.app.ui.auth

sealed class AuthSignInResult {
    data object Success : AuthSignInResult()
    data object MfaRequired : AuthSignInResult()
    data class Error(val message: String) : AuthSignInResult()
}

expect object FirebaseAuthClient {
    suspend fun signIn(email: String, password: String): AuthSignInResult
    suspend fun verifyTotp(otp: String): AuthSignInResult
}
