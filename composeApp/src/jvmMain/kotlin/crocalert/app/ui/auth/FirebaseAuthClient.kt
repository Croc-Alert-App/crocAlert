package crocalert.app.ui.auth

actual object FirebaseAuthClient {
    actual suspend fun signIn(email: String, password: String): AuthSignInResult =
        AuthSignInResult.Error("Firebase Auth is not supported on desktop")

    actual suspend fun verifyTotp(otp: String): AuthSignInResult =
        AuthSignInResult.Error("Firebase Auth is not supported on desktop")

    actual suspend fun register(email: String, password: String): AuthSignInResult =
        AuthSignInResult.Error("Firebase Auth is not supported on desktop")

    actual suspend fun sendVerificationEmail(): AuthSignInResult =
        AuthSignInResult.Error("Firebase Auth is not supported on desktop")

    actual suspend fun generateTotpSetup(): TotpSetupResult =
        TotpSetupResult.Error("Firebase Auth is not supported on desktop")

    actual suspend fun enrollTotp(otp: String): AuthSignInResult =
        AuthSignInResult.Error("Firebase Auth is not supported on desktop")
}
