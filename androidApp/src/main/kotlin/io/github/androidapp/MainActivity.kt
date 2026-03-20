package io.github.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import crocalert.app.App
import crocalert.app.shared.AppModule
import crocalert.app.theme.CrocAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppModule.setup()
        enableEdgeToEdge()
        setContent {
            CrocAlertTheme {
                App()
            }
        }
    }
}
