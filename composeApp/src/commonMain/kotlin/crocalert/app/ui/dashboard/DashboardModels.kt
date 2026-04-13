package crocalert.app.ui.dashboard

data class DashboardData(
    val activeCameras: Int,
    val totalCameras: Int,
    val networkHealthPct: Float,
    val activeAlerts: Int,
    val alertWindowDays: Int,
    val captureRate: String,
    val captureRatePct: Float,
    val networkTrend: List<NetworkTrendDay>,
    val recentActivity: List<ActivityEvent>
)

data class NetworkTrendDay(val label: String, val value: Float, val isToday: Boolean = false)

data class ActivityEvent(
    val title: String,
    val timeAgo: String,
    val severity: String,
    val isNew: Boolean = false,
    val alertId: String? = null,
)

// Navigation and sync state — defined here so ViewModels never import from UI component files.
enum class DashboardTab { Home, Cameras, Alerts, Profile }

enum class SyncStatus { Syncing, Synced, Error }
