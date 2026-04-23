package crocalert.app.ui.dashboard

import crocalert.app.model.Alert

data class DashboardData(
    val activeCameras: Int,
    val totalCameras: Int,
    val networkHealthPct: Float,
    val activeAlertas: Int,
    val activePreAlertas: Int,
    val alertWindowDays: Int,
    val captureRate: String,
    val captureRatePct: Float,
    val recentActivity: List<Alert>
)

// Navigation and sync state — defined here so ViewModels never import from UI component files.
enum class DashboardTab { Home, Cameras, Alerts, Profile }

enum class SyncStatus { Syncing, Synced, Error }
