package crocalert.app.ui.auth

actual object FirebaseAuthClient {
    actual suspend fun signIn(email: String, password: String): AuthSignInResult =
        AuthSignInResult.Error("Firebase Auth is not supported on desktop")

    actual suspend fun verifyTotp(otp: String): AuthSignInResult =
        AuthSignInResult.Error("Firebase Auth is not supported on desktop")
}
