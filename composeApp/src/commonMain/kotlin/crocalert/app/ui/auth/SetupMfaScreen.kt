package crocalert.app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocBlue
import crocalert.app.ui.auth.components.AuthScreenScaffold
import crocalert.app.ui.auth.components.CrocAlertPrimaryButton
import crocalert.app.ui.auth.components.CrocAlertSecondaryButton

@Composable
fun SetupMfaScreen(
    onStartSetup: () -> Unit,
    onSkip: () -> Unit,
) {
    AuthScreenScaffold {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "CONFIGURAR MFA",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CrocBlue,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Para proteger tu cuenta, vamos a configurar una app autenticadora con códigos de 6 dígitos.",
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(32.dp))
        CrocAlertPrimaryButton(
            text = "Configurar ahora",
            onClick = onStartSetup,
        )
        Spacer(Modifier.height(12.dp))
        CrocAlertSecondaryButton(
            text = "Más tarde",
            onClick = onSkip,
        )
    }
}