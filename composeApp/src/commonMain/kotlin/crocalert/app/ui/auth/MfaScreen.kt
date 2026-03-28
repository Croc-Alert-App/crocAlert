package crocalert.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocBlueVibrant
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.auth.components.CrocAlertPrimaryButton
import crocalert.app.ui.auth.components.OtpInputField
import kotlinx.coroutines.delay

private val MfaBannerColor = Color(0xFFD6E8FF)

@Composable
fun MfaScreen(
    maskedDevice: String = "••89",
    onVerify: (code: String) -> Unit,
    onResend: () -> Unit,
    onUseBackupCode: () -> Unit,
) {
    var otp by remember { mutableStateOf(FakeAuth.OTP) }
    var countdownSeconds by remember { mutableStateOf(42) }

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            delay(1_000L)
            countdownSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrocWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "VERIFICAR IDENTIDAD",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CrocBlue,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Ingresa el código de 6 dígitos enviado a tu aplicación o dispositivo de autenticación, que termina en $maskedDevice",
                fontSize = 13.sp,
                color = CrocNeutralDark,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(32.dp))
            OtpInputField(
                value = otp,
                onValueChange = { otp = it },
            )
            Spacer(Modifier.height(32.dp))
            CrocAlertPrimaryButton(
                text = "Verify Code",
                onClick = { onVerify(otp) },
                enabled = otp.length == 6,
            )
            Spacer(Modifier.height(20.dp))
            ResendRow(
                countdownSeconds = countdownSeconds,
                onResend = onResend,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Utilice un código de respaldo en su lugar",
                fontSize = 13.sp,
                color = CrocNeutralDark,
                modifier = Modifier.clickable { onUseBackupCode() },
            )
        }

        // MFA required banner — always pinned to bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            color = MfaBannerColor,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "Se requiere MFA para todo acceso al campo.",
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = CrocBlue,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ResendRow(countdownSeconds: Int, onResend: () -> Unit) {
    Row {
        Text(
            text = "¿No recibiste el código? ",
            fontSize = 13.sp,
            color = CrocNeutralDark,
        )
        if (countdownSeconds > 0) {
            Text(
                text = "Reenviar(${countdownSeconds.toCountdown()})",
                fontSize = 13.sp,
                color = CrocBlueVibrant,
                textDecoration = TextDecoration.Underline,
            )
        } else {
            Text(
                text = "Reenviar",
                fontSize = 13.sp,
                color = CrocBlueVibrant,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onResend() },
            )
        }
    }
}

private fun Int.toCountdown(): String {
    val m = this / 60
    val s = this % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
