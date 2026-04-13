package io.github.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import crocalert.app.App
import crocalert.app.shared.AppModule
import crocalert.app.theme.CrocAlertTheme
import crocalert.app.ui.auth.DataStoreSessionPreferences
import crocalert.app.ui.auth.SessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppModule.setup(applicationContext)
        SessionManager.init(DataStoreSessionPreferences(applicationContext))
        enableEdgeToEdge()
        setContent {
            CrocAlertTheme {
                App()
            }
        }
    }
}
