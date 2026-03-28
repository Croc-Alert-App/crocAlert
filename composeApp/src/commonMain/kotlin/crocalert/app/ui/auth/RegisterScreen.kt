package crocalert.app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
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

private val USER_ROLES = listOf("Administrador", "Experto SINAC")

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
// Min 8 chars · 1 uppercase · 1 lowercase · 1 digit
private val PASSWORD_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$")

@Composable
fun RegisterScreen(
    onRegister: (nombre: String, apellidos: String, email: String, rol: String, password: String) -> Unit,
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
    var rolDirty by remember { mutableStateOf(false) }

    // Derived error messages
    val nombreError = if (nombreDirty && nombre.isBlank()) "Este campo es requerido" else null
    val apellidosError = if (apellidosDirty && apellidos.isBlank()) "Este campo es requerido" else null
    val emailError = when {
        email.isEmpty() -> null
        !email.matches(EMAIL_REGEX) -> "Correo electrónico inválido"
        else -> null
    }
    val passwordError = when {
        password.isEmpty() -> null
        !password.matches(PASSWORD_REGEX) ->
            "Mínimo 8 caracteres, una mayúscula, una minúscula y un número"
        else -> null
    }
    val confirmError = when {
        confirmPassword.isEmpty() -> null
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
            placeholder = "worker@sinac.go.cr",
            isError = nombreError != null,
            errorMessage = nombreError,
        )
        Spacer(Modifier.height(16.dp))
        CrocAlertTextField(
            value = apellidos,
            onValueChange = { apellidos = it; apellidosDirty = true },
            label = "APELLIDOS",
            placeholder = "worker@sinac.go.cr",
            isError = apellidosError != null,
            errorMessage = apellidosError,
        )
        Spacer(Modifier.height(16.dp))
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
            onValueChange = { password = it },
            label = "CONTRASEÑA",
            isError = passwordError != null,
            errorMessage = passwordError,
        )
        Spacer(Modifier.height(16.dp))
        CrocAlertPasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "CONFIRMACION DE CONTRASEÑA",
            isError = confirmError != null,
            errorMessage = confirmError,
        )
        Spacer(Modifier.height(32.dp))
        CrocAlertPrimaryButton(
            text = "Registrarse",
            onClick = { onRegister(nombre, apellidos, email, rol, password) },
            enabled = isFormValid,
        )
    }
}
