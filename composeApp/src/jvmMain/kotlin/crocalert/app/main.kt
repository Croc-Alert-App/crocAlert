package crocalert.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import crocalert.app.theme.CrocAlertTheme

fun main() = application {
    // TODO: pass baseUrl = "http://localhost:8080" to createAlertRepository() when real data is wired up.
    Window(
        onCloseRequest = ::exitApplication,
        title = "CrocAlert",
    ) {
        CrocAlertTheme {
            App()
        }
    }
}
