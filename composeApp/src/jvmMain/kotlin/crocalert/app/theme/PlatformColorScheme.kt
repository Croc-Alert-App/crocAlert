package crocalert.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun platformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme =
    if (darkTheme) DarkColorScheme else LightColorScheme
