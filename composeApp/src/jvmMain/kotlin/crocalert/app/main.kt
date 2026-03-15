package crocalert.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.theme.CrocAlertTheme

fun main() = application {
    ApiRoutes.BASE = "http://localhost:8080"
    Window(
        onCloseRequest = ::exitApplication,
        title = "CrocAlert",
    ) {
        CrocAlertTheme {
            App()
        }
    }
}
