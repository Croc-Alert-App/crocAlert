package crocalert.app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocBlue
import crocalert.app.ui.auth.components.AuthScreenScaffold
import crocalert.app.ui.auth.components.CrocAlertDropdownField
import crocalert.app.ui.auth.components.CrocAlertPasswordField
import crocalert.app.ui.auth.components.CrocAlertPrimaryButton
import crocalert.app.ui.auth.components.CrocAlertTextField
import crocalert.app.ui.auth.components.PasswordRequirementsHint

private val USER_ROLES = listOf("Administrador", "Experto SINAC")

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
// Min 8 chars · 1 uppercase · 1 lowercase · 1 digit
private val PASSWORD_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$")

@Composable
fun RegisterScreen(
    isLoading: Boolean = false,
    registerError: String? = null,
    registerSuccess: Boolean = false,
    onRegister: (nombre: String, apellidos: String, email: String, rol: String, password: String) -> Unit,
    onSuccessDismiss: () -> Unit,
    onErrorDismiss: () -> Unit = {},
) {
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Dirty flags — errors only show after the user touches each field
    var nombreDirty by remember { mutableStateOf(false) }
    var apellidosDirty by remember { mutableStateOf(false) }
    var emailDirty by remember { mutableStateOf(false) }
    var passwordDirty by remember { mutableStateOf(false) }
    var confirmDirty by remember { mutableStateOf(false) }
    var rolDirty by remember { mutableStateOf(false) }

    // Firebase errors that map to a specific field are shown inline on that field;
    // everything else falls through to the generic bottom banner.
    val isEmailTaken = registerError == "Este correo ya está registrado."

    // Derived error messages
    val nombreError = if (nombreDirty && nombre.isBlank()) "Este campo es requerido" else null
    val apellidosError = if (apellidosDirty && apellidos.isBlank()) "Este campo es requerido" else null
    val emailError = when {
        isEmailTaken -> registerError
        !emailDirty || email.isEmpty() -> null
        !email.matches(EMAIL_REGEX) -> "Correo electrónico inválido"
        else -> null
    }
    val passwordInvalid = passwordDirty && password.isNotEmpty() && !password.matches(PASSWORD_REGEX)
    val confirmError = when {
        !confirmDirty || confirmPassword.isEmpty() -> null
        confirmPassword != password -> "Las contraseñas no coinciden"
        else -> null
    }
    val rolError = if (rolDirty && rol.isEmpty()) "Selecciona un rol" else null

    val isFormValid by remember {
        derivedStateOf {
            nombre.isNotBlank() &&
                apellidos.isNotBlank() &&
                email.matches(EMAIL_REGEX) &&
                rol.isNotEmpty() &&
                password.matches(PASSWORD_REGEX) &&
                confirmPassword == password
        }
    }

    // Success modal
    if (registerSuccess) {
        AlertDialog(
            onDismissRequest = { /* require explicit button tap */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CrocBlue,
                )
            },
            title = {
                Text(
                    text = "¡Cuenta creada!",
                    fontWeight = FontWeight.Bold,
                    color = CrocBlue,
                )
            },
            text = {
                Text(
                    "Te enviamos un correo de verificación a $email.\n\n" +
                    "Revisa tu bandeja de entrada y haz clic en el enlace para activar tu cuenta. " +
                    "Después podrás iniciar sesión y configurar la autenticación de dos pasos."
                )
            },
            confirmButton = {
                TextButton(onClick = onSuccessDismiss) {
                    Text(
                        text = "Ir a iniciar sesión",
                        color = CrocBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
        )
    }

    AuthScreenScaffold {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "CREAR CUENTA",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CrocBlue,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(32.dp))
        CrocAlertTextField(
            value = nombre,
            onValueChange = { nombre = it; nombreDirty = true },
            label = "NOMBRE",
            placeholder = "Nombre",
            isError = nombreError != null,
            errorMessage = nombreError,
        )
        Spacer(Modifier.height(16.dp))
        CrocAlertTextField(
            value = apellidos,
            onValueChange = { apellidos = it; apellidosDirty = true },
            label = "APELLIDOS",
            placeholder = "Apellidos",
            isError = apellidosError != null,
            errorMessage = apellidosError,
        )
        Spacer(Modifier.height(16.dp))
        CrocAlertTextField(
            value = email,
            onValueChange = {
                email = it
                emailDirty = true
                if (isEmailTaken) onErrorDismiss()
            },
            label = "CORREO ELECTRONICO",
            placeholder = "corre_electronico@ejemplo.com",
            keyboardType = KeyboardType.Email,
            isError = emailError != null,
            errorMessage = emailError,
        )
        Spacer(Modifier.height(16.dp))
        CrocAlertDropdownField(
            value = rol,
            onValueChange = { rol = it; rolDirty = true },
            label = "ROL DE USUARIO",
            options = USER_ROLES,
            placeholder = "Selecciona un rol",
            isError = rolError != null,
            errorMessage = rolError,
        )
        Spacer(Modifier.height(16.dp))
        CrocAlertPasswordField(
            value = password,
            onValueChange = { password = it; passwordDirty = true },
            label = "CONTRASEÑA",
            placeholder = "Crea tu contraseña",
            isError = passwordInvalid,
        )
        PasswordRequirementsHint(password = password, dirty = passwordDirty)
        Spacer(Modifier.height(16.dp))
        CrocAlertPasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; confirmDirty = true },
            label = "CONFIRMACION DE CONTRASEÑA",
            placeholder = "Repite tu contraseña",
            isError = confirmError != null,
            errorMessage = confirmError,
        )
        if (registerError != null && !isEmailTaken) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = registerError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
        if (isLoading) {
            CircularProgressIndicator(color = CrocBlue)
        } else {
            CrocAlertPrimaryButton(
                text = "Registrarse",
                onClick = { onRegister(nombre, apellidos, email, rol, password) },
                enabled = isFormValid,
            )
        }
    }
}
