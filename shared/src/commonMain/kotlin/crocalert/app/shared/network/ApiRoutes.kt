package crocalert.app.shared.network

object ApiRoutes {
    // Emulator default — override per platform. TODO: wire BuildConfig.API_BASE_URL before release.
    const val DEFAULT_BASE = "http://10.0.2.2:8080"

    /** API key sent in X-API-Key header. Set before calling createAlertRepository(). */
    var API_KEY: String = ""

    fun alertsUrl(baseUrl: String) = "$baseUrl/alerts"
}