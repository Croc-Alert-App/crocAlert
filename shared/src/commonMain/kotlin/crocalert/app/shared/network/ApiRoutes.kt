package crocalert.app.shared.network

object ApiRoutes {
    // Empty by design — each platform's AppModule must supply its own base URL.
    // Using this default on iOS or Desktop will produce an immediate, visible failure
    // rather than silently connecting to the Android emulator loopback alias.
    const val DEFAULT_BASE = ""

    /** Base URL for the API. Set before calling createAlertRepository(). */
    var BASE: String = DEFAULT_BASE

    /** API key sent in X-API-Key header. Set before calling createAlertRepository(). */
    var API_KEY: String = ""

    fun alertsUrl(baseUrl: String) = "$baseUrl/alerts"
    fun camerasUrl(baseUrl: String) = "$baseUrl/cameras"
    fun capturesByCameraUrl(baseUrl: String, cameraId: String) = "$baseUrl/captures/by-camera/$cameraId"
    fun sitesUrl(baseUrl: String) = "$baseUrl/sites"
    fun dailyStatsUrl(baseUrl: String, cameraId: String, date: String) = "$baseUrl/cameras/$cameraId/daily-stats/$date"
    fun allDailyStatsUrl(baseUrl: String, date: String) = "$baseUrl/cameras/daily-stats/$date"
    fun monitoringDashboardUrl(baseUrl: String, date: String) = "$baseUrl/cameras/dashboard/$date"
    fun globalDailyCaptureRateUrl(baseUrl: String, date: String) = "$baseUrl/cameras/global-daily-rate/$date"
}
