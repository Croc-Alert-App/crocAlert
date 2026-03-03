package crocalert.app.shared.network

import io.ktor.client.call.body
import io.ktor.client.request.get

suspend fun ping(baseUrl: String): String {
    val client = HttpClientFactory.create()
    return client.get("$baseUrl/ping").body()
}