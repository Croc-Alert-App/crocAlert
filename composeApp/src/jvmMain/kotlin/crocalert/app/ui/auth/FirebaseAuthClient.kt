package crocalert.app.ui.auth

import java.util.prefs.Preferences
import crocalert.app.shared.UserSession
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

actual object FirebaseAuthClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }
    }

    private const val API_KEY = "AIzaSyAnLwBMrsXmnVIa4VW9tHwa-MF7A8sPYqI"

    private val prefs = Preferences.userRoot().node("crocalert_auth")

    private object Keys {
        const val ID_TOKEN = "id_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "display_name"
    }

    private var currentIdToken: String? = null
    private var currentRefreshToken: String? = null
    private var currentEmail: String? = null
    private var currentDisplayName: String? = null

    actual suspend fun signIn(email: String, password: String): AuthSignInResult {
        return try {
            val response = client.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword") {
                parameter("key", API_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    SignInRequest(
                        email = email,
                        password = password,
                        returnSecureToken = true
                    )
                )
            }.body<SignInResponse>()

            currentIdToken = response.idToken
            currentRefreshToken = response.refreshToken
            currentEmail = response.email

            val user = lookupUser(response.idToken)
                ?: return AuthSignInResult.Error("No se pudo obtener la sesión del usuario.")

            if (user.emailVerified == false) {
                return AuthSignInResult.EmailNotVerified
            }

            currentDisplayName = user.displayName
            UserSession.populate(
                user.displayName,
                user.email ?: (currentEmail ?: ""),
                extractRoleFromDisplayName(user.displayName)
            )

            persistSession()
            AuthSignInResult.Success
        } catch (e: ClientRequestException) {
            val raw = e.response.bodyAsText()
            AuthSignInResult.Error(mapFirebaseError(raw))
        } catch (e: Exception) {
            AuthSignInResult.Error("Operación fallida. Intenta de nuevo.")
        }
    }

    actual suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: String,
    ): AuthSignInResult {
        return try {
            val response = client.post("https://identitytoolkit.googleapis.com/v1/accounts:signUp") {
                parameter("key", API_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    SignUpRequest(
                        email = email,
                        password = password,
                        returnSecureToken = true
                    )
                )
            }.body<SignUpResponse>()

            currentIdToken = response.idToken
            currentRefreshToken = response.refreshToken
            currentEmail = response.email

            val encodedDisplayName = UserSession.encode(fullName, role)
            currentDisplayName = encodedDisplayName

            client.post("https://identitytoolkit.googleapis.com/v1/accounts:update") {
                parameter("key", API_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateProfileRequest(
                        idToken = response.idToken,
                        displayName = encodedDisplayName,
                        returnSecureToken = true
                    )
                )
            }.body<UpdateProfileResponse>()

            client.post("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode") {
                parameter("key", API_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    SendOobCodeRequest(
                        requestType = "VERIFY_EMAIL",
                        idToken = response.idToken
                    )
                )
            }

            clearPersistedSession()
            UserSession.clear()

            AuthSignInResult.Success
        } catch (e: ClientRequestException) {
            val raw = e.response.bodyAsText()
            AuthSignInResult.Error(mapFirebaseError(raw))
        } catch (e: Exception) {
            AuthSignInResult.Error("Operación fallida. Intenta de nuevo.")
        }
    }

    actual suspend fun sendVerificationEmail(): AuthSignInResult {
        val idToken = currentIdToken
            ?: return AuthSignInResult.Error("No hay sesión activa. Intenta iniciar sesión de nuevo.")

        return try {
            client.post("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode") {
                parameter("key", API_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    SendOobCodeRequest(
                        requestType = "VERIFY_EMAIL",
                        idToken = idToken
                    )
                )
            }

            clearPersistedSession()
            UserSession.clear()

            AuthSignInResult.Success
        } catch (e: ClientRequestException) {
            clearPersistedSession()
            UserSession.clear()

            val raw = e.response.bodyAsText()
            AuthSignInResult.Error(mapFirebaseError(raw))
        } catch (e: Exception) {
            clearPersistedSession()
            UserSession.clear()

            AuthSignInResult.Error("No se pudo enviar el correo de verificación.")
        }
    }

    actual suspend fun sendPasswordReset(email: String): AuthSignInResult {
        return try {
            client.post("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode") {
                parameter("key", API_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    SendOobCodeRequest(
                        requestType = "PASSWORD_RESET",
                        email = email
                    )
                )
            }
            AuthSignInResult.Success
        } catch (e: ClientRequestException) {
            val raw = e.response.bodyAsText()
            AuthSignInResult.Error(mapFirebaseError(raw))
        } catch (e: Exception) {
            AuthSignInResult.Error("No se pudo enviar el correo. Intenta de nuevo.")
        }
    }

    actual suspend fun verifyTotp(otp: String): AuthSignInResult =
        AuthSignInResult.Error(
            "MFA TOTP de desktop aún no implementado. " +
                    "Este flujo requiere los endpoints MFA de Identity Platform."
        )

    actual suspend fun generateTotpSetup(): TotpSetupResult =
        TotpSetupResult.Error(
            "MFA TOTP de desktop aún no implementado. " +
                    "Este flujo requiere los endpoints MFA de Identity Platform."
        )

    actual suspend fun enrollTotp(otp: String): AuthSignInResult =
        AuthSignInResult.Error(
            "MFA TOTP de desktop aún no implementado. " +
                    "Este flujo requiere los endpoints MFA de Identity Platform."
        )

    actual suspend fun restoreSession(): Boolean {
        loadPersistedSession()

        val idToken = currentIdToken ?: return false

        return try {
            val user = lookupUser(idToken) ?: run {
                clearPersistedSession()
                return false
            }

            currentEmail = user.email ?: currentEmail
            currentDisplayName = user.displayName ?: currentDisplayName

            UserSession.populate(
                user.displayName ?: currentDisplayName,
                user.email ?: (currentEmail ?: ""),
                extractRoleFromDisplayName(user.displayName ?: currentDisplayName)
            )
            true
        } catch (_: Exception) {
            clearPersistedSession()
            false
        }
    }

    // Logout real desde este mismo archivo.
    // Puedes llamarlo manualmente desde desktop cuando agregues el botón.
    suspend fun signOut() {
        clearPersistedSession()
        UserSession.clear()
    }

    private fun persistSession() {
        if (currentIdToken == null) prefs.remove(Keys.ID_TOKEN) else prefs.put(Keys.ID_TOKEN, currentIdToken)
        if (currentRefreshToken == null) prefs.remove(Keys.REFRESH_TOKEN) else prefs.put(Keys.REFRESH_TOKEN, currentRefreshToken)
        if (currentEmail == null) prefs.remove(Keys.EMAIL) else prefs.put(Keys.EMAIL, currentEmail)
        if (currentDisplayName == null) prefs.remove(Keys.DISPLAY_NAME) else prefs.put(Keys.DISPLAY_NAME, currentDisplayName)
    }

    private fun loadPersistedSession() {
        currentIdToken = prefs.get(Keys.ID_TOKEN, null)
        currentRefreshToken = prefs.get(Keys.REFRESH_TOKEN, null)
        currentEmail = prefs.get(Keys.EMAIL, null)
        currentDisplayName = prefs.get(Keys.DISPLAY_NAME, null)
    }

    private fun clearPersistedSession() {
        prefs.remove(Keys.ID_TOKEN)
        prefs.remove(Keys.REFRESH_TOKEN)
        prefs.remove(Keys.EMAIL)
        prefs.remove(Keys.DISPLAY_NAME)

        currentIdToken = null
        currentRefreshToken = null
        currentEmail = null
        currentDisplayName = null
    }

    private suspend fun lookupUser(idToken: String): LookupUser? {
        val response = client.post("https://identitytoolkit.googleapis.com/v1/accounts:lookup") {
            parameter("key", API_KEY)
            contentType(ContentType.Application.Json)
            setBody(LookupRequest(idToken))
        }.body<LookupResponse>()

        return response.users.firstOrNull()
    }

    private fun extractRoleFromDisplayName(displayName: String?): String? {
        if (displayName.isNullOrBlank()) return null
        return displayName.substringAfter("::", missingDelimiterValue = "").ifBlank { null }
    }

    private fun mapFirebaseError(raw: String?): String {
        val text = raw.orEmpty()

        return when {
            "EMAIL_NOT_FOUND" in text -> "No existe una cuenta con este correo."
            "INVALID_PASSWORD" in text -> "Contraseña incorrecta."
            "INVALID_LOGIN_CREDENTIALS" in text -> "Credenciales incorrectas. Verifica tu correo y contraseña."
            "USER_DISABLED" in text -> "Esta cuenta ha sido deshabilitada."
            "EMAIL_EXISTS" in text -> "Este correo ya está registrado."
            "TOO_MANY_ATTEMPTS_TRY_LATER" in text -> "Demasiados intentos. Intenta más tarde."
            "NETWORK_REQUEST_FAILED" in text -> "Sin conexión a internet. Verifica tu red."
            "WEAK_PASSWORD" in text -> "La contraseña es demasiado débil."
            else -> "Operación fallida. Intenta de nuevo."
        }
    }
}

@Serializable
private data class SignInRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean
)

@Serializable
private data class SignInResponse(
    val localId: String,
    val email: String,
    val displayName: String? = null,
    val idToken: String,
    val refreshToken: String
)

@Serializable
private data class SignUpRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean
)

@Serializable
private data class SignUpResponse(
    val localId: String,
    val email: String,
    val idToken: String,
    val refreshToken: String
)

@Serializable
private data class UpdateProfileRequest(
    val idToken: String,
    val displayName: String,
    val returnSecureToken: Boolean
)

@Serializable
private data class UpdateProfileResponse(
    val displayName: String? = null,
    val idToken: String? = null,
    val refreshToken: String? = null
)

@Serializable
private data class SendOobCodeRequest(
    val requestType: String,
    val email: String? = null,
    val idToken: String? = null
)

@Serializable
private data class LookupRequest(
    val idToken: String
)

@Serializable
private data class LookupResponse(
    val users: List<LookupUser> = emptyList()
)

@Serializable
private data class LookupUser(
    val localId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    @SerialName("emailVerified")
    val emailVerified: Boolean? = null
)