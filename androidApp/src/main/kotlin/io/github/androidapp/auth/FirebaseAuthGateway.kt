package io.github.androidapp.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpSecret
import crocalert.app.ui.auth.AuthGateway
import crocalert.app.ui.auth.AuthResult
import crocalert.app.ui.auth.AuthUser
import crocalert.app.ui.auth.TotpEnrollmentInfo
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuthMultiFactorException

class FirebaseAuthGateway(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthGateway {

    private var pendingTotpSecret: TotpSecret? = null
    private var pendingMfaException: FirebaseAuthMultiFactorException? = null

    override fun currentUser(): AuthUser? {
        val user = firebaseAuth.currentUser ?: return null
        return AuthUser(
            uid = user.uid,
            email = user.email.orEmpty(),
            displayName = user.displayName,
            emailVerified = user.isEmailVerified,
        )
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult<AuthUser> {
        return try {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email.trim(), password)
                .await()

            val user = result.user
                ?: return AuthResult.Error("No se pudo obtener el usuario autenticado.")

            AuthResult.Success(
                AuthUser(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    displayName = user.displayName,
                    emailVerified = user.isEmailVerified,
                ),
            )
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthMultiFactorException -> {
                    pendingMfaException = e
                    AuthResult.MfaRequired
                }

                else -> {
                    val message = when (e) {
                        is FirebaseAuthInvalidUserException -> "Usuario no registrado."
                        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
                        is FirebaseTooManyRequestsException -> "Demasiados intentos. Intenta más tarde."
                        is FirebaseNetworkException -> "Revisa tu conexión a internet."
                        else -> e.message ?: "No se pudo iniciar sesión."
                    }
                    AuthResult.Error(message)
                }
            }
        }
    }

    override suspend fun register(
        nombre: String,
        apellidos: String,
        email: String,
        rol: String,
        password: String,
    ): AuthResult<AuthUser> {
        return try {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email.trim(), password)
                .await()

            val user = result.user
                ?: return AuthResult.Error("No se pudo crear la cuenta.")

            val fullName = listOf(nombre.trim(), apellidos.trim())
                .filter { it.isNotBlank() }
                .joinToString(" ")

            if (fullName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()

                user.updateProfile(profileUpdates).await()
            }

            user.sendEmailVerification().await()
            user.reload().await()

            val refreshedUser = firebaseAuth.currentUser
                ?: return AuthResult.Error("La cuenta se creó, pero no se pudo refrescar la sesión.")

            AuthResult.Success(
                AuthUser(
                    uid = refreshedUser.uid,
                    email = refreshedUser.email.orEmpty(),
                    displayName = refreshedUser.displayName,
                    emailVerified = refreshedUser.isEmailVerified,
                ),
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "No se pudo registrar la cuenta.")
        }
    }

    override suspend fun sendPasswordReset(
        email: String,
    ): AuthResult<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "No se pudo enviar el correo de recuperación.")
        }
    }

    override suspend fun startTotpEnrollment(): AuthResult<TotpEnrollmentInfo> {
        val user = firebaseAuth.currentUser
            ?: return AuthResult.Error("No hay una sesión activa.")

        if (!user.isEmailVerified) {
            return AuthResult.Error("Debes verificar tu correo antes de activar MFA.")
        }

        return try {
            val multiFactorSession = user.multiFactor.session.await()
            val secret = TotpMultiFactorGenerator.generateSecret(multiFactorSession).await()
            pendingTotpSecret = secret

            val qrCodeUrl = secret.generateQrCodeUrl(
                user.email.orEmpty(),
                "CrocAlert",
            )

            AuthResult.Success(
                TotpEnrollmentInfo(
                    secretKey = secret.sharedSecretKey,
                    qrCodeUrl = qrCodeUrl,
                ),
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "No se pudo iniciar la configuración de MFA.")
        }
    }

    override suspend fun finalizeTotpEnrollment(
        verificationCode: String,
    ): AuthResult<Unit> {
        val user = firebaseAuth.currentUser
            ?: return AuthResult.Error("No hay una sesión activa.")

        val secret = pendingTotpSecret
            ?: return AuthResult.Error("Primero debes iniciar la configuración de MFA.")

        return try {
            val assertion = TotpMultiFactorGenerator.getAssertionForEnrollment(
                secret,
                verificationCode,
            )

            user.multiFactor.enroll(assertion, "Authenticator App").await()
            pendingTotpSecret = null
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "No se pudo confirmar MFA.")
        }
    }

    override suspend fun finalizeTotpSignIn(
        verificationCode: String,
    ): AuthResult<Unit> {
        val exception = pendingMfaException
            ?: return AuthResult.Error("No hay un desafío MFA pendiente.")

        return try {
            val hint = exception.resolver.hints.firstOrNull()
                ?: return AuthResult.Error("No se encontró un factor MFA disponible.")

            val assertion = TotpMultiFactorGenerator.getAssertionForSignIn(
                hint.uid,
                verificationCode,
            )

            exception.resolver.resolveSignIn(assertion).await()
            pendingMfaException = null
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "No se pudo completar el inicio de sesión MFA.")
        }
    }

    override fun isTotpEnrolled(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return user.multiFactor.enrolledFactors.isNotEmpty()
    }

    override fun signOut() {
        pendingTotpSecret = null
        firebaseAuth.signOut()
    }
}