package crocalert.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.theme.CrocAlertTheme
import crocalert.app.ui.auth.SessionManager
import crocalert.app.ui.auth.DesktopSessionPreferences

fun main() = application {
    ApiRoutes.BASE = System.getenv("CROCALERT_API_URL") ?: "http://localhost:8080"

    SessionManager.init(DesktopSessionPreferences())

    Window(
        onCloseRequest = ::exitApplication,
        title = "CrocAlert"
    ) {
        CrocAlertTheme {
            App()
        }
    }
}