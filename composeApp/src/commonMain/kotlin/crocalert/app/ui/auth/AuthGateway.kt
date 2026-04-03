package crocalert.app.ui.auth
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val emailVerified: Boolean = false,
)

data class TotpEnrollmentInfo(
    val secretKey: String,
    val qrCodeUrl: String,
)

sealed interface AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>
    data class Error(val message: String) : AuthResult<Nothing>
    data object MfaRequired : AuthResult<Nothing>
}

interface AuthGateway {
    fun currentUser(): AuthUser?

    suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult<AuthUser>

    suspend fun register(
        nombre: String,
        apellidos: String,
        email: String,
        rol: String,
        password: String,
    ): AuthResult<AuthUser>

    suspend fun sendPasswordReset(
        email: String,
    ): AuthResult<Unit>

    suspend fun startTotpEnrollment(): AuthResult<TotpEnrollmentInfo>

    suspend fun finalizeTotpEnrollment(
        verificationCode: String,
    ): AuthResult<Unit>

    fun isTotpEnrolled(): Boolean

    suspend fun finalizeTotpSignIn(
        verificationCode: String,
    ): AuthResult<Unit>

    fun signOut()

}

