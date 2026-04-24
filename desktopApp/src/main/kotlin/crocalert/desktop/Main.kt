package crocalert.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import crocalert.app.App
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.theme.CrocAlertTheme
import crocalert.app.ui.auth.SessionManager
import crocalert.app.ui.auth.SessionPreferences

private object AlwaysActiveSessionPreferences : SessionPreferences {
    // Returns a session expiry one year from launch — effectively permanent.
    // Avoids Long.MAX_VALUE arithmetic overflow in sessionRemainingMs().
    override suspend fun getSessionExpiresAt(): Long =
        System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000

    override suspend fun getSavedEmail(): String? = null
    override suspend fun setSavedEmail(email: String?) {}
    override suspend fun setSessionExpiresAt(expiresAt: Long?) {}
    override suspend fun updateSession(email: String?, expiresAt: Long?) {}
}

fun main() {
    SessionManager.init(AlwaysActiveSessionPreferences)
    application {
        ApiRoutes.BASE = System.getenv("CROCALERT_API_URL") ?: "http://localhost:8080"

        Window(
            onCloseRequest = ::exitApplication,
            title = "CrocAlert"
        ) {
            CrocAlertTheme {
                App()
            }
        }
    }
}
