package crocalert.app.ui.dashboard

import crocalert.app.model.Alert

data class DashboardData(
    val activeCameras: Int,
    val totalCameras: Int,
    val networkHealthPct: Float,
    val activeAlertas: Int,
    val activePreAlertas: Int,
    val captureRate: String,
    val captureRatePct: Float,
    val recentActivity: List<Alert>
)

sealed class DashboardFilter {
    data class LastDays(val days: Int) : DashboardFilter()
    data class Custom(val startMs: Long, val endMs: Long) : DashboardFilter()
}

// Navigation and sync state — defined here so ViewModels never import from UI component files.
enum class DashboardTab { Home, Cameras, Alerts, Profile }

enum class SyncStatus { Syncing, Synced, Error }
