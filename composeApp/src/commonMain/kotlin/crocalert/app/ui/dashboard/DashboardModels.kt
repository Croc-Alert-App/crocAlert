package crocalert.app.ui.dashboard

data class DashboardData(
    val activeCameras: Int,
    val totalCameras: Int,
    val networkHealthPct: Float,
    val activeAlertas: Int,
    val activePreAlertas: Int,
    val alertWindowDays: Int,
    val captureRate: String,
    val captureRatePct: Float,
    val recentActivity: List<ActivityEvent>
)

data class ActivityEvent(
    val title: String,
    val timeAgo: String,
    val severity: String,
    val isNew: Boolean = false,
    val alertId: String? = null,
    val folder: String? = null,
)

// Navigation and sync state — defined here so ViewModels never import from UI component files.
enum class DashboardTab { Home, Cameras, Alerts, Profile }

enum class SyncStatus { Syncing, Synced, Error }
