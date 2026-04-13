package crocalert.app.ui.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpMultiFactorInfo
import com.google.firebase.auth.TotpSecret
import com.google.firebase.auth.UserProfileChangeRequest
import crocalert.app.shared.UserSession
import kotlinx.coroutines.tasks.await

actual object FirebaseAuthClient {

    private var pendingResolver: MultiFactorResolver? = null
    private var pendingTotpSecret: TotpSecret? = null

    actual suspend fun signIn(email: String, password: String): AuthSignInResult {
        return try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            val user = FirebaseAuth.getInstance().currentUser

            if (user?.isEmailVerified == false) {
                return AuthSignInResult.EmailNotVerified
            }

            // Read the `role` custom claim from the ID token (set via Firebase Admin SDK/console).
            // This is the authoritative role source for accounts that predate displayName encoding.
            // forceRefresh=true ensures we get a fresh token with the latest custom claims,
            // not a cached one that may predate an admin role assignment.
            val roleFromClaims = user?.getIdToken(true)?.await()?.claims?.get("role") as? String

            val enrolled = user?.multiFactor?.enrolledFactors
            if (enrolled.isNullOrEmpty()) {
                UserSession.populate(user?.displayName, user?.email ?: email, roleFromClaims)
                AuthSignInResult.MfaEnrollmentRequired
            } else {
                UserSession.populate(user?.displayName, user?.email ?: email, roleFromClaims)
                AuthSignInResult.Success
            }
        } catch (e: FirebaseAuthMultiFactorException) {
            pendingResolver = e.resolver
            AuthSignInResult.MfaRequired
        } catch (e: FirebaseAuthException) {
            AuthSignInResult.Error(signInErrorMessage(e.errorCode))
        } catch (e: Exception) {
            AuthSignInResult.Error("Inicio de sesión fallido. Intenta de nuevo.")
        }
    }

    actual suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: String,
    ): AuthSignInResult {
        return try {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
            val user = FirebaseAuth.getInstance().currentUser

            // Store full name + role in Firebase displayName as "FullName::Role"
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(UserSession.encode(fullName, role))
                .build()
            user?.updateProfile(profileUpdate)?.await()

            user?.sendEmailVerification()?.await()
            FirebaseAuth.getInstance().signOut()
            AuthSignInResult.Success
        } catch (e: FirebaseAuthException) {
            AuthSignInResult.Error(registerErrorMessage(e.errorCode))
        } catch (e: Exception) {
            AuthSignInResult.Error("Registro fallido. Intenta de nuevo.")
        }
    }

    actual suspend fun sendVerificationEmail(): AuthSignInResult {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return AuthSignInResult.Error(
                "No hay sesión activa. Intenta iniciar sesión de nuevo para reenviar el correo."
            )
        return try {
            user.sendEmailVerification().await()
            FirebaseAuth.getInstance().signOut()
            AuthSignInResult.Success
        } catch (e: FirebaseAuthException) {
            FirebaseAuth.getInstance().signOut()
            AuthSignInResult.Error(
                when (e.errorCode) {
                    "ERROR_TOO_MANY_REQUESTS"      -> "Ya enviamos un correo recientemente. Espera unos minutos."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión a internet. Verifica tu red."
                    else -> "No se pudo enviar el correo de verificación."
                }
            )
        } catch (e: Exception) {
            FirebaseAuth.getInstance().signOut()
            AuthSignInResult.Error("No se pudo enviar el correo de verificación.")
        }
    }

    actual suspend fun verifyTotp(otp: String): AuthSignInResult {
        val resolver = pendingResolver
            ?: return AuthSignInResult.Error("Sesión expirada. Por favor, inicia sesión de nuevo.")
        return try {
            val hint = resolver.hints.filterIsInstance<TotpMultiFactorInfo>().firstOrNull()
                ?: return AuthSignInResult.Error("No se encontró un método MFA TOTP válido.")
            val assertion = TotpMultiFactorGenerator.getAssertionForSignIn(hint.uid, otp)
            resolver.resolveSignIn(assertion).await()
            pendingResolver = null
            // Populate session after TOTP verification (MFA path)
            val user = FirebaseAuth.getInstance().currentUser
            // forceRefresh=true ensures we get a fresh token with the latest custom claims,
            // not a cached one that may predate an admin role assignment.
            val roleFromClaims = user?.getIdToken(true)?.await()?.claims?.get("role") as? String
            UserSession.populate(user?.displayName, user?.email ?: "", roleFromClaims)
            AuthSignInResult.Success
        } catch (e: FirebaseAuthException) {
            AuthSignInResult.Error(totpErrorMessage(e.errorCode))
        } catch (e: Exception) {
            AuthSignInResult.Error("Verificación fallida. Intenta de nuevo.")
        }
    }

    actual suspend fun generateTotpSetup(): TotpSetupResult {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return TotpSetupResult.Error("Sesión no encontrada. Inicia sesión de nuevo.")
        return try {
            val session = user.multiFactor.session.await()
            val secret = TotpMultiFactorGenerator.generateSecret(session).await()
            pendingTotpSecret = secret
            val uri = secret.generateQrCodeUrl(user.email ?: "usuario", "CrocAlert")
            TotpSetupResult.Success(uri = uri, secretKey = secret.sharedSecretKey)
        } catch (e: FirebaseAuthException) {
            val message = when (e.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "MFA TOTP no está habilitado en el proyecto. Actívalo en la consola de Firebase."
                "ERROR_USER_TOKEN_EXPIRED", "ERROR_REQUIRES_RECENT_LOGIN" ->
                    "Tu sesión expiró. Inicia sesión de nuevo para continuar."
                else -> "No se pudo generar la configuración MFA (${e.errorCode})."
            }
            TotpSetupResult.Error(message)
        } catch (e: Exception) {
            TotpSetupResult.Error("No se pudo generar la configuración MFA: ${e.message}")
        }
    }

    actual suspend fun enrollTotp(otp: String): AuthSignInResult {
        val secret = pendingTotpSecret
            ?: return AuthSignInResult.Error("Sesión expirada. Inicia sesión de nuevo.")
        val user = FirebaseAuth.getInstance().currentUser
            ?: return AuthSignInResult.Error("Sesión no encontrada. Inicia sesión de nuevo.")
        return try {
            val assertion = TotpMultiFactorGenerator.getAssertionForEnrollment(secret, otp)
            user.multiFactor.enroll(assertion, "CrocAlert TOTP").await()
            pendingTotpSecret = null
            // Session already populated during signIn (before MFA enrollment)
            AuthSignInResult.Success
        } catch (e: FirebaseAuthException) {
            AuthSignInResult.Error(totpErrorMessage(e.errorCode))
        } catch (e: Exception) {
            AuthSignInResult.Error("Error al activar MFA. Intenta de nuevo.")
        }
    }

    private fun signInErrorMessage(errorCode: String): String = when (errorCode) {
        "ERROR_INVALID_EMAIL"          -> "El correo electrónico no es válido."
        "ERROR_USER_NOT_FOUND"         -> "No existe una cuenta con este correo."
        "ERROR_WRONG_PASSWORD"         -> "Contraseña incorrecta."
        "ERROR_INVALID_CREDENTIAL"     -> "Credenciales incorrectas. Verifica tu correo y contraseña."
        "ERROR_USER_DISABLED"          -> "Esta cuenta ha sido deshabilitada."
        "ERROR_TOO_MANY_REQUESTS"      -> "Demasiados intentos fallidos. Intenta más tarde."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión a internet. Verifica tu red."
        "ERROR_OPERATION_NOT_ALLOWED"  -> "Este método de inicio de sesión no está habilitado."
        else                           -> "Inicio de sesión fallido. Intenta de nuevo."
    }

    private fun registerErrorMessage(errorCode: String): String = when (errorCode) {
        "ERROR_INVALID_EMAIL"          -> "El correo electrónico no es válido."
        "ERROR_EMAIL_ALREADY_IN_USE"   -> "Este correo ya está registrado."
        "ERROR_WEAK_PASSWORD"          -> "La contraseña es demasiado débil."
        "ERROR_OPERATION_NOT_ALLOWED"  -> "El registro con correo y contraseña no está habilitado."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión a internet. Verifica tu red."
        "ERROR_TOO_MANY_REQUESTS"      -> "Demasiados intentos. Intenta más tarde."
        else                           -> "Registro fallido. Intenta de nuevo."
    }

    private fun totpErrorMessage(errorCode: String): String = when (errorCode) {
        "ERROR_INVALID_VERIFICATION_CODE" -> "Código incorrecto. Verifica e intenta de nuevo."
        "ERROR_SESSION_EXPIRED"           -> "Sesión expirada. Por favor, inicia sesión de nuevo."
        "ERROR_TOO_MANY_REQUESTS"         -> "Demasiados intentos. Intenta más tarde."
        "ERROR_NETWORK_REQUEST_FAILED"    -> "Sin conexión a internet. Verifica tu red."
        else                              -> "Verificación fallida. Intenta de nuevo."
    }
}
