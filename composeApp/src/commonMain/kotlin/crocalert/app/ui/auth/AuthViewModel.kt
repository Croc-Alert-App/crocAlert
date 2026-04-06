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

    /**
     * Signs in with email/password.
     * - On MFA required: calls [onMfaRequired].
     * - On success (no MFA enrolled): calls [onSuccess].
     * - On error: exposes [loginError].
     */
    fun login(
        email: String,
        password: String,
        rememberDevice: Boolean,
        onMfaRequired: () -> Unit,
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
                is AuthSignInResult.Success -> {
                    if (rememberDevice) SessionManager.rememberDevice()
                    onSuccess()
                }
                is AuthSignInResult.Error -> _loginError.value = result.message
            }
            _isLoading.value = false
        }
    }

    /**
     * Resolves TOTP MFA with the given [otp].
     * - On success: calls [onSuccess].
     * - On error: exposes [mfaError].
     */
    fun verifyTotp(otp: String, onSuccess: () -> Unit) {
        _mfaError.value = null
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = FirebaseAuthClient.verifyTotp(otp)) {
                is AuthSignInResult.Success -> onSuccess()
                is AuthSignInResult.Error -> _mfaError.value = result.message
                is AuthSignInResult.MfaRequired -> Unit // unreachable from verifyTotp
            }
            _isLoading.value = false
        }
    }

    fun clearLoginError() { _loginError.value = null }
    fun clearMfaError() { _mfaError.value = null }
}
