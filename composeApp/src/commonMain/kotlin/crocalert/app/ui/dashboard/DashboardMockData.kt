package crocalert.app.ui.dashboard

/**
 * Temporary mock data for the dashboard.
 * Replace [load] with a real repository/API call when the endpoint is available.
 * Delete this file once the remote data source is wired up.
 */
internal object DashboardMockData {
    fun load(): DashboardData = DashboardData(
        activeCameras = 24,
        networkHealthPct = 0.92f,
        activeAlerts = 3,
        criticalAlerts = 1,
        captureRate = "18/24h",
        captureRatePct = 0.75f,
        integrityPct = 0.991f,
        networkTrend = listOf(
            // 7-day lookback ending today — no future dates after isToday
            NetworkTrendDay("V", 0.40f),
            NetworkTrendDay("S", 0.45f),
            NetworkTrendDay("D", 0.35f),
            NetworkTrendDay("L", 0.60f),
            NetworkTrendDay("M", 0.50f),
            NetworkTrendDay("X", 0.70f),
            NetworkTrendDay("J", 1.00f, isToday = true)
        ),
        metadataMetrics = listOf(
            MetricItem("Precisión del GPS", 0.985f, "98.5%"),
            MetricItem("Integridad de la marca de tiempo", 1.00f, "100%"),
            MetricItem("Integridad de la imagen", 0.942f, "94.2%")
        ),
        recentActivity = listOf(
            ActivityEvent("Conexión perdida: CAM-12", "Hace 15 minutos", "Crítico", isNew = true),
            ActivityEvent("Captura correcta: CAM-04", "Hace 2 minutos", "Integridad OK")
        )
    )
}
