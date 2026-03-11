package crocalert.app

import androidx.compose.runtime.Composable
import crocalert.app.feature.alerts.ui.AlertListScreen
import crocalert.app.theme.CrocAlertTheme

@Composable
fun App() {
    CrocAlertTheme {
        AlertListScreen()
    }
}
