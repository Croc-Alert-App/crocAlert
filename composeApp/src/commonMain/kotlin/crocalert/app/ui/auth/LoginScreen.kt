package crocalert.app.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocAmber
import crocalert.app.theme.CrocBlack
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocBlueVibrant
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.ui.auth.components.AuthDivider
import crocalert.app.ui.auth.components.AuthScreenScaffold
import crocalert.app.ui.auth.components.CrocAlertPasswordField
import crocalert.app.ui.auth.components.CrocAlertPrimaryButton
import crocalert.app.ui.auth.components.CrocAlertSecondaryButton
import crocalert.app.ui.auth.components.CrocAlertTextField

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String, rememberDevice: Boolean) -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    var email by remember { mutableStateOf(FakeAuth.EMAIL) }
    var password by remember { mutableStateOf(FakeAuth.PASSWORD) }
    var rememberDevice by remember { mutableStateOf(false) }

    val emailError = if (email.isNotEmpty() && !email.matches(EMAIL_REGEX))
        "Correo electrónico inválido" else null

    val isFormValid by remember {
        derivedStateOf {
            email.matches(EMAIL_REGEX) && password.isNotEmpty()
        }
    }

    AuthScreenScaffold {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "¡Ingresa a CrocAlert!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CrocBlue,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(32.dp))
        CrocAlertTextField(
            value = email,
            onValueChange = { email = it },
            label = "CORREO ELECTRONICO",
            placeholder = "worker@sinac.go.cr",
            keyboardType = KeyboardType.Email,
            isError = emailError != null,
            errorMessage = emailError,
        )
        Spacer(Modifier.height(16.dp))
        CrocAlertPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "CONTRASEÑA",
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberDevice,
                    onCheckedChange = { rememberDevice = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CrocAmber,
                        checkmarkColor = CrocBlack,
                    ),
                )
                Text(
                    text = "Recordar dispositivo",
                    fontSize = 13.sp,
                    color = CrocNeutralDark,
                )
            }
            Text(
                text = "Recuperar contraseña?",
                fontSize = 13.sp,
                color = CrocBlueVibrant,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onForgotPassword() },
            )
        }
        Spacer(Modifier.height(24.dp))
        CrocAlertPrimaryButton(
            text = "Iniciar sesión",
            onClick = { onLogin(email, password, rememberDevice) },
            enabled = isFormValid,
        )
        Spacer(Modifier.height(16.dp))
        AuthDivider()
        Spacer(Modifier.height(16.dp))
        CrocAlertSecondaryButton(
            text = "Registrarse",
            onClick = onRegister,
        )
    }
}
