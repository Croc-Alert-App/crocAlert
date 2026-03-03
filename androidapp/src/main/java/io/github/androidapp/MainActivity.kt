package io.github.androidapp
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp
import io.ktor.client.call.body
import io.ktor.client.request.get
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import crocalert.app.shared.network.HttpClientFactory
import crocalert.app.shared.network.ping
import io.github.androidapp.ui.theme.CrocAlertTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // ✅
        enableEdgeToEdge()

        //  test conexión al server
        lifecycleScope.launch {
            try {
                val resp = ping("http://10.0.2.2:8080")
                println("RESPUESTA DEL SERVER: $resp")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        setContent {
            CrocAlertTheme {

                var result by remember { mutableStateOf("Listo") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {

                        Button(onClick = {
                            result = "CLICK ✅, llamando al server..."

                            lifecycleScope.launch {
                                try {
                                    val client = HttpClientFactory.create()
                                    val resp: String = client.get("http://10.0.2.2:8080/alerts").body()
                                    result = "OK ✅: $resp"
                                    android.util.Log.d("CrocAlert", "RESP: $resp")
                                } catch (e: Exception) {
                                    result = "ERROR ❌: ${e::class.simpleName} - ${e.message}"
                                    android.util.Log.e("CrocAlert", "ERROR", e)
                                }
                            }
                        }) { Text("Cargar alerts") }

                        Spacer(Modifier.height(12.dp))

                        Text(result)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CrocAlertTheme {
        Greeting("Android")
    }
}