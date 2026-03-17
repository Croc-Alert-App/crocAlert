package io.github.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import crocalert.app.App
import crocalert.app.theme.CrocAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // ApiRoutes.BASE already defaults to the emulator alias (10.0.2.2:8080).
        // For a real device or CI, override here with the server's LAN/cloud URL,
        // ideally via BuildConfig.API_BASE_URL wired from local.properties.
        setContent {
            CrocAlertTheme {
                App()
            }
        }
    }
}
