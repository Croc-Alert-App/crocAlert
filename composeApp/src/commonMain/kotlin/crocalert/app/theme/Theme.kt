package crocalert.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val LightColorScheme = lightColorScheme(
    primary          = CrocBlue,
    onPrimary        = CrocWhite,
    primaryContainer = CrocBlueLight,
    secondary        = CrocBlueVibrant,
    onSecondary      = CrocWhite,
    tertiary         = CrocAmber,
    onTertiary       = CrocBlack,
)

internal val DarkColorScheme = darkColorScheme(
    primary          = CrocBlueLight,
    onPrimary        = CrocBlue,
    primaryContainer = CrocBlue,
    secondary        = CrocBlueVibrant,
    onSecondary      = CrocWhite,
    tertiary         = CrocAmber,
    onTertiary       = CrocBlack,
)

/**
 * CrocAlert brand theme. [dynamicColor] is honoured on Android 12+; ignored on all other targets.
 */
@Composable
fun CrocAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = platformColorScheme(darkTheme, dynamicColor)
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CrocAlertTypography,
        content     = content,
    )
}

/** Resolved per-platform so Android can opt in to Material You dynamic colours. */
@Composable
internal expect fun platformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme
