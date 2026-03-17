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
        // Override ApiRoutes.BASE here for real-device or CI builds (see BuildConfig.API_BASE_URL).
        setContent {
            CrocAlertTheme {
                App()
            }
        }
    }
}
