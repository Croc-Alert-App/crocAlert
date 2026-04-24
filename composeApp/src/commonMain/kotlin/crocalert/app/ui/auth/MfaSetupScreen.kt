package crocalert.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocBlueLight
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.theme.CrocNeutralLight
import crocalert.app.theme.CrocWhite
import crocalert.app.ui.auth.components.CrocAlertPrimaryButton
import crocalert.app.ui.auth.components.OtpInputField
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import androidx.compose.foundation.Image

private val MfaBannerColor = Color(0xFFD6E8FF)

@Composable
fun MfaSetupScreen(
    isLoading: Boolean = false,
    setupData: TotpSetupResult.Success? = null,
    setupError: String? = null,
    enrollError: String? = null,
    onGenerateSetup: () -> Unit,
    onEnroll: (otp: String) -> Unit,
    onEnrollErrorDismiss: () -> Unit = {},
) {
    var otp by remember { mutableStateOf("") }

    // Trigger QR generation as soon as the screen appears
    LaunchedEffect(Unit) { onGenerateSetup() }

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
            Text(
                text = "ACTIVAR AUTENTICACIÓN",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CrocBlue,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tu cuenta requiere verificación en dos pasos.\nSigue los pasos para activarla.",
                fontSize = 13.sp,
                color = CrocNeutralDark,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(28.dp))

            // ── Step 1 ──────────────────────────────────────────────────────
            StepLabel(number = "1", text = "Instala Google Authenticator en tu teléfono.")
            Spacer(Modifier.height(20.dp))

            // ── Step 2 — QR code ────────────────────────────────────────────
            StepLabel(number = "2", text = "Escanea el código QR con la app.")
            Spacer(Modifier.height(16.dp))

            when {
                isLoading && setupData == null -> {
                    CircularProgressIndicator(color = CrocBlue, modifier = Modifier.size(48.dp))
                }
                setupError != null -> {
                    Text(
                        text = setupError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                setupData != null -> {
                    QrCodeCard(uri = setupData.uri)
                    Spacer(Modifier.height(12.dp))
                    SecretKeyCard(secretKey = setupData.secretKey)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Step 3 — OTP ────────────────────────────────────────────────
            StepLabel(number = "3", text = "Ingresa el código de 6 dígitos que muestra la app.")
            Spacer(Modifier.height(16.dp))

            OtpInputField(
                value = otp,
                onValueChange = { otp = it; onEnrollErrorDismiss() },
            )

            if (enrollError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = enrollError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))

            if (isLoading && setupData != null) {
                CircularProgressIndicator(color = CrocBlue)
            } else {
                CrocAlertPrimaryButton(
                    text = "Activar autenticación",
                    onClick = { onEnroll(otp) },
                    enabled = otp.length == 6 && setupData != null && !isLoading,
                )
            }
        }

        // Required MFA banner pinned to bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            color = MfaBannerColor,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "Se requiere MFA para todo acceso al sistema.",
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
private fun QrCodeCard(uri: String) {
    val painter = rememberQrCodePainter(
        data = uri,
        shapes = QrShapes(
            ball = QrBallShape.roundCorners(0.25f),
            darkPixel = QrPixelShape.roundCorners(0.5f),
            frame = QrFrameShape.roundCorners(0.25f),
        ),
    )
    Box(
        modifier = Modifier
            .size(200.dp)
            .border(1.dp, CrocNeutralLight, RoundedCornerShape(12.dp))
            .background(CrocWhite, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = "Código QR para Google Authenticator",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SecretKeyCard(secretKey: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "¿No puedes escanear? Ingresa esta clave manualmente:",
            fontSize = 12.sp,
            color = CrocNeutralDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CrocBlueLight.copy(alpha = 0.25f),
        ) {
            Text(
                text = secretKey.chunked(4).joinToString(" "),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CrocBlue,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StepLabel(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(CrocBlue, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CrocWhite,
            )
        }
        Text(
            text = text,
            fontSize = 14.sp,
            color = CrocNeutralDark,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
