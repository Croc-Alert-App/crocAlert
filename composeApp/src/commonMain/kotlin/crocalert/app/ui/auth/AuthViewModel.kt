package crocalert.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _mfaError = MutableStateFlow<String?>(null)
    val mfaError: StateFlow<String?> = _mfaError

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError

    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess

    /** Non-null once [generateTotpSetup] succeeds — holds the URI and raw secret key. */
    private val _totpSetup = MutableStateFlow<TotpSetupResult.Success?>(null)
    val totpSetup: StateFlow<TotpSetupResult.Success?> = _totpSetup

    private val _totpSetupError = MutableStateFlow<String?>(null)
    val totpSetupError: StateFlow<String?> = _totpSetupError

    private val _enrollError = MutableStateFlow<String?>(null)
    val enrollError: StateFlow<String?> = _enrollError

    /**
     * Signs in with email/password.
     * - MFA already enrolled → [onMfaRequired] (verify existing TOTP)
     * - MFA not yet enrolled → [onMfaEnrollmentRequired] (force setup)
     * - No MFA at all and account is exempt → [onSuccess] (should not happen in prod)
     */
    fun login(
        email: String,
        password: String,
        rememberDevice: Boolean,
        onMfaRequired: () -> Unit,
        onMfaEnrollmentRequired: () -> Unit,
        onSuccess: () -> Unit,
    ) {
        _loginError.value = null
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = FirebaseAuthClient.signIn(email, password)) {
                is AuthSignInResult.MfaRequired -> {
                    if (rememberDevice) SessionManager.rememberDevice()
                    onMfaRequired()
                }
                is AuthSignInResult.MfaEnrollmentRequired -> {
                    if (rememberDevice) SessionManager.rememberDevice()
                    onMfaEnrollmentRequired()
                }
                is AuthSignInResult.Success -> {
                    if (rememberDevice) SessionManager.rememberDevice()
                    onSuccess()
                }
                is AuthSignInResult.EmailNotVerified -> {
                    // Send a fresh verification email every time the user tries to log in
                    // while unverified. currentUser is still active at this point.
                    FirebaseAuthClient.sendVerificationEmail()
                    _loginError.value =
                        "Revisa tu correo electrónico y haz clic en el enlace de verificación " +
                        "para activar tu cuenta. Si no lo encuentras, revisa tu carpeta de spam."
                }
                is AuthSignInResult.Error -> _loginError.value = result.message
            }
            _isLoading.value = false
        }
    }

    /**
     * Resolves TOTP MFA with the given [otp] (sign-in verification, not enrollment).
     */
    fun verifyTotp(otp: String, onSuccess: () -> Unit) {
        _mfaError.value = null
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = FirebaseAuthClient.verifyTotp(otp)) {
                is AuthSignInResult.Success -> onSuccess()
                is AuthSignInResult.Error -> _mfaError.value = result.message
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    /**
     * Creates a new Firebase Auth account with email/password.
     */
    fun register(
        nombre: String,
        apellidos: String,
        email: String,
        rol: String,
        password: String,
    ) {
        _registerError.value = null
        _isLoading.value = true
        viewModelScope.launch {
            val fullName = "$nombre $apellidos".trim()
            when (val result = FirebaseAuthClient.register(email, password, fullName, rol)) {
                is AuthSignInResult.Success -> _registerSuccess.value = true
                is AuthSignInResult.Error -> _registerError.value = result.message
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    /**
     * Fetches the TOTP secret and QR URI for first-time MFA enrollment.
     * Call this once when the MFA setup screen appears.
     */
    fun generateTotpSetup() {
        _totpSetupError.value = null
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = FirebaseAuthClient.generateTotpSetup()) {
                is TotpSetupResult.Success -> _totpSetup.value = result
                is TotpSetupResult.Error -> _totpSetupError.value = result.message
            }
            _isLoading.value = false
        }
    }

    /**
     * Completes TOTP enrollment with the [otp] the user entered from Google Authenticator.
     */
    fun enrollTotp(otp: String, onSuccess: () -> Unit) {
        _enrollError.value = null
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = FirebaseAuthClient.enrollTotp(otp)) {
                is AuthSignInResult.Success -> onSuccess()
                is AuthSignInResult.Error -> _enrollError.value = result.message
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    fun clearLoginError() { _loginError.value = null }
    fun clearMfaError() { _mfaError.value = null }
    fun clearRegisterError() { _registerError.value = null }
    fun clearRegisterSuccess() { _registerSuccess.value = false }
    fun clearEnrollError() { _enrollError.value = null }
    fun clearTotpSetup() { _totpSetup.value = null }
}
