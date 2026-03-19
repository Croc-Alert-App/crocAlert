/*package crocalert.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import crocalert.app.theme.CrocAlertTheme

fun main() = application {
    // Desktop server runs on localhost. Pass the URL to createAlertRepository() here
    // when real data is wired up: createAlertRepository(baseUrl = "http://localhost:8080")
    Window(
        onCloseRequest = ::exitApplication,
        title = "CrocAlert",
    ) {
        CrocAlertTheme {
            App()
        }
    }
}
*/