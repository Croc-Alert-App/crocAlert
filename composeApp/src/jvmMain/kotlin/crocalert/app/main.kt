package crocalert.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import crocalert.app.theme.CrocAlertTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CrocAlert",
    ) {
        CrocAlertTheme {
            App()
        }
    }
}
