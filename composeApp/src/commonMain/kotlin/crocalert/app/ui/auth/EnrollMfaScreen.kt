package crocalert.app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocBlue
import crocalert.app.ui.auth.components.AuthScreenScaffold
import crocalert.app.ui.auth.components.CrocAlertPrimaryButton
import crocalert.app.ui.auth.components.CrocAlertSecondaryButton
import crocalert.app.ui.auth.components.CrocAlertTextField

@Composable
fun EnrollMfaScreen(
    enrollmentInfo: TotpEnrollmentInfo,
    onConfirmCode: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var code by remember { mutableStateOf("") }

    val canContinue by remember {
        derivedStateOf { code.length == 6 && code.all { it.isDigit() } }
    }

    AuthScreenScaffold {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "ACTIVAR MFA",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CrocBlue,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Abre Google Authenticator, Authy o una app similar y agrega una cuenta nueva con esta clave secreta.",
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Clave secreta",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CrocBlue,
        )
        Spacer(Modifier.height(8.dp))
        SelectionContainer {
            Text(
                text = enrollmentInfo.secretKey,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Después ingresa aquí el código de 6 dígitos generado por tu app autenticadora.",
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))
        CrocAlertTextField(
            value = code,
            onValueChange = {
                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                    code = it
                }
            },
            label = "CÓDIGO DE VERIFICACIÓN",
            placeholder = "123456",
        )
        Spacer(Modifier.height(24.dp))
        CrocAlertPrimaryButton(
            text = "Confirmar MFA",
            onClick = { onConfirmCode(code) },
            enabled = canContinue,
        )
        Spacer(Modifier.height(12.dp))
        CrocAlertSecondaryButton(
            text = "Cancelar",
            onClick = onCancel,
        )
    }
}