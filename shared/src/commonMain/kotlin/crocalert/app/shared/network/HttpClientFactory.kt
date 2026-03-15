package crocalert.app.shared.network

import io.ktor.client.HttpClient

expect object HttpClientFactory {
    fun create(): HttpClient
}