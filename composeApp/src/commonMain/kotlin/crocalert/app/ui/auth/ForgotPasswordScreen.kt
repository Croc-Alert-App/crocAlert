package crocalert.app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocBlueVibrant
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.ui.auth.components.AuthScreenScaffold
import crocalert.app.ui.auth.components.CrocAlertPrimaryButton
import crocalert.app.ui.auth.components.CrocAlertSecondaryButton
import crocalert.app.ui.auth.components.CrocAlertTextField
import androidx.compose.runtime.rememberCoroutineScope
import crocalert.app.ui.auth.AuthGateway
import crocalert.app.ui.auth.AuthResult
import kotlinx.coroutines.launch
private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

@Composable
fun ForgotPasswordScreen(
    authGateway: AuthGateway,
    onBack: () -> Unit,
)  {
    var email by remember { mutableStateOf("") }
    var emailSent by remember { mutableStateOf(false) }

    val emailError = if (email.isNotEmpty() && !email.matches(EMAIL_REGEX))
        "Correo electrónico inválido" else null
    val scope = rememberCoroutineScope()

    AuthScreenScaffold {
        Spacer(Modifier.height(40.dp))

        if (emailSent) {
            SuccessContent(email = email, onBack = onBack)
        } else {
            RequestContent(
                email = email,
                emailError = emailError,
                onEmailChange = { email = it },
                onSend = {
                    scope.launch {
                        when (authGateway.sendPasswordReset(email)) {
                            is AuthResult.Success -> {
                                emailSent = true
                            }
                            is AuthResult.Error -> {
                                emailSent = true
                            }

                            AuthResult.MfaRequired -> Unit
                        }
                    }
                },
                onBack = onBack,
            )
        }
    }
}

// ── Request state ─────────────────────────────────────────────────────────────

@Composable
private fun RequestContent(
    email: String,
    emailError: String?,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    Text(
        text = "RECUPERAR CONTRASEÑA",
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        color = CrocBlue,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña.",
        fontSize = 13.sp,
        color = CrocNeutralDark,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
    )
    Spacer(Modifier.height(32.dp))
    CrocAlertTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "CORREO ELECTRONICO",
        placeholder = "worker@sinac.go.cr",
        keyboardType = KeyboardType.Email,
        isError = emailError != null,
        errorMessage = emailError,
    )
    Spacer(Modifier.height(24.dp))
    CrocAlertPrimaryButton(
        text = "Enviar instrucciones",
        onClick = onSend,
        enabled = email.matches(EMAIL_REGEX),
    )
    Spacer(Modifier.height(12.dp))
    CrocAlertSecondaryButton(
        text = "Volver al inicio de sesión",
        onClick = onBack,
    )
}

// ── Success state ─────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(email: String, onBack: () -> Unit) {
    Icon(
        imageVector = Icons.Default.MarkEmailRead,
        contentDescription = null,
        tint = CrocBlueVibrant,
        modifier = Modifier.size(64.dp),
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = "REVISA TU CORREO",
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        color = CrocBlue,
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Si existe una cuenta para $email, recibirás un enlace para restablecer tu contraseña en los próximos minutos.",
        fontSize = 13.sp,
        color = CrocNeutralDark,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
    )
    Spacer(Modifier.height(32.dp))
    CrocAlertPrimaryButton(
        text = "Volver al inicio de sesión",
        onClick = onBack,
    )
}
