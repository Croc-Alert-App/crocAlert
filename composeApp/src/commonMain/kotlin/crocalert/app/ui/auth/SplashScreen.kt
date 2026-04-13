package crocalert.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import crocalert.app.theme.CrocBlue
import crocalert.app.theme.CrocBlueVibrant
import crocalert.app.theme.CrocNeutralDark
import crocalert.app.theme.CrocWhite
import kotlinx.coroutines.delay

enum class SessionCheckResult { Active, Expired, None }

@Composable
fun SplashScreen(onSessionChecked: (SessionCheckResult) -> Unit) {
    var debugText by remember { mutableStateOf("...") }

    LaunchedEffect(Unit) {
        debugText = SessionManager.debugInfo()
        delay(2_000L)
        val result = when {
            SessionManager.isSessionValid()   -> SessionCheckResult.Active
            SessionManager.isSessionExpired() -> SessionCheckResult.Expired
            else                              -> SessionCheckResult.None
        }
        onSessionChecked(result)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrocWhite),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.LightGray, RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "CROC ALERT",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CrocBlue,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sistema de Monitoreo de Campo v1.0",
                fontSize = 14.sp,
                color = CrocNeutralDark,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            CircularProgressIndicator(
                color = CrocBlueVibrant,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Comprobando sesión...",
                fontSize = 14.sp,
                color = CrocBlueVibrant,
            )
            Spacer(Modifier.height(8.dp))
            // DEBUG — remove before release
            Text(
                text = debugText,
                fontSize = 10.sp,
                color = CrocNeutralDark,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = "Croc Alert · Protected System",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            fontSize = 11.sp,
            color = CrocNeutralDark,
        )
    }
}
