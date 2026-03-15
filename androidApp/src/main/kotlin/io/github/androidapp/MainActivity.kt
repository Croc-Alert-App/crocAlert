package io.github.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import crocalert.app.App
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.theme.CrocAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Android emulator routes localhost through 10.0.2.2
        // Change to your machine's LAN IP when testing on a real device
        ApiRoutes.BASE = "http://10.0.2.2:8080"
        setContent {
            CrocAlertTheme {
                App()
            }
        }
    }
}
