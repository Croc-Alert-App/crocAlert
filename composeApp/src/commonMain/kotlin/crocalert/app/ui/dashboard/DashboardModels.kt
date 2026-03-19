package crocalert.app.ui.dashboard

data class DashboardData(
    val activeCameras: Int,
    val networkHealthPct: Float,
    val activeAlerts: Int,
    val criticalAlerts: Int,
    val captureRate: String,
    val captureRatePct: Float,
    val integrityPct: Float,
    val networkTrend: List<NetworkTrendDay>,
    val metadataMetrics: List<MetricItem>,
    val recentActivity: List<ActivityEvent>
)

data class NetworkTrendDay(val label: String, val value: Float, val isToday: Boolean = false)

data class MetricItem(val name: String, val progress: Float, val displayValue: String)

data class ActivityEvent(
    val title: String,
    val timeAgo: String,
    val severity: String,
    val isNew: Boolean = false
)

// Navigation and sync state — defined here so ViewModels never import from UI component files.
enum class DashboardTab { Home, Cameras, Alerts, Profile }

enum class SyncStatus { Syncing, Synced, Error }
