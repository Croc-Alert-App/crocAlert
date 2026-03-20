package crocalert.app.feature.alerts.data

import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.model.AlertType

/**
 * Centralized hardcoded mock data for the Alerts List feature.
 *
 * All sample alerts are defined here so they can be reused across
 * MockAlertRepository, UI previews, and unit tests without duplication.
 *
 * Timestamps use a fixed base epoch so sort order is deterministic in tests.
 * BASE_EPOCH_MS ≈ 2026-03-09 09:00 UTC.
 */
object AlertSampleData {

    private const val BASE_EPOCH_MS = 1741514400000L // 2026-03-09 09:00 UTC

    val alerts: List<Alert> = listOf(
        Alert(
            id = "alert-001",
            title = "Possible Crocodile Detected",
            message = "High-confidence detection near the south riverbank. Immediate on-site inspection is recommended.",
            type = AlertType.POSSIBLE_CROCODILE,
            priority = AlertPriority.CRITICAL,
            status = AlertStatus.OPEN,
            createdAt = BASE_EPOCH_MS,
            sourceName = "CAM-12 Río Conchal",
            isRead = false,
        ),
        Alert(
            id = "alert-002",
            title = "Motion Detected Near Riverbank",
            message = "Unusual motion pattern detected in restricted area. Flagged for review.",
            type = AlertType.MOTION_DETECTED,
            priority = AlertPriority.HIGH,
            status = AlertStatus.IN_PROGRESS,
            createdAt = BASE_EPOCH_MS - 720_000L,       // 12 min before base
            sourceName = "CAM-08 Laguna Norte",
            isRead = false,
        ),
        Alert(
            id = "alert-003",
            title = "New Image Captured",
            message = "New image uploaded from remote camera. Pending AI analysis.",
            type = AlertType.IMAGE_UPLOADED,
            priority = AlertPriority.MEDIUM,
            status = AlertStatus.OPEN,
            createdAt = BASE_EPOCH_MS - 2_700_000L,     // 45 min before base
            sourceName = "CAM-03 Sector Este",
            isRead = true,
        ),
        Alert(
            id = "alert-004",
            title = "Device Communication Warning",
            message = "Camera has not reported status in over 2 hours. Check network connectivity.",
            type = AlertType.SYSTEM_WARNING,
            priority = AlertPriority.MEDIUM,
            status = AlertStatus.IN_PROGRESS,
            createdAt = BASE_EPOCH_MS - 9_000_000L,     // 2.5 hr before base
            sourceName = "CAM-05 Margen Sur",
            isRead = true,
        ),
        Alert(
            id = "alert-005",
            title = "Battery Low on Remote Camera",
            message = "Battery level at 8%. Device may go offline within 30 minutes.",
            type = AlertType.BATTERY_LOW,
            priority = AlertPriority.LOW,
            status = AlertStatus.OPEN,
            createdAt = BASE_EPOCH_MS - 39_600_000L,    // 11 hr before base
            sourceName = "CAM-11 Acceso Norte",
            isRead = true,
        ),
        Alert(
            id = "alert-006",
            title = "Sync Completed Successfully",
            message = "All camera records synchronized. 248 captures uploaded to the server.",
            type = AlertType.SYNC_COMPLETED,
            priority = AlertPriority.LOW,
            status = AlertStatus.CLOSED,
            createdAt = BASE_EPOCH_MS - 64_800_000L,    // 18 hr before base
            sourceName = "System",
            isRead = true,
        ),
    )
}
