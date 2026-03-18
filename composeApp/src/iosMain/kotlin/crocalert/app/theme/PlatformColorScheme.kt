package crocalert.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// iOS has no Material You dynamic colours — dark/light scheme only.
@Composable
internal actual fun platformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme =
    if (darkTheme) DarkColorScheme else LightColorScheme
