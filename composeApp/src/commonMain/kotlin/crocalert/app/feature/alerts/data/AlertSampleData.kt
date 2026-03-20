package crocalert.app.feature.alerts.data

import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.model.AlertType
import kotlinx.datetime.Clock

/**
 * Centralized mock data for the Alerts List feature.
 *
 * Timestamps are relative to [Clock.System.now()] so date-based filters
 * always return predictable subsets regardless of when the app is opened.
 *
 * Distribution (9 alerts total):
 *
 *  Id       Priority   Age              TODAY  WEEK  MONTH  ALL
 *  001      CRITICAL   just now           ✓      ✓     ✓     ✓
 *  002      HIGH       12 min ago         ✓      ✓     ✓     ✓
 *  003      MEDIUM     45 min ago         ✓      ✓     ✓     ✓
 *  004      MEDIUM     2.5 hr ago         ✓      ✓     ✓     ✓
 *  007      CRITICAL   1 day ago          ✗      ✓     ✓     ✓
 *  008      HIGH       2 days ago         ✗      ✓     ✓     ✓
 *  009      MEDIUM     8 days ago         ✗      ✗     ✓     ✓
 *  005      LOW        3 days ago         ✗      ✓     ✓     ✓  (not shown in tabs)
 *  006      LOW        12 days ago        ✗      ✗     ✓     ✓  (not shown in tabs)
 *
 * LOW priority alerts exist in the raw data but are intentionally excluded
 * from both tabs (Alerts = CRITICAL+HIGH, Pre-Alerts = MEDIUM).
 */
object AlertSampleData {

    val alerts: List<Alert>
        get() {
            val now = Clock.System.now().toEpochMilliseconds()
            return listOf(
                Alert(
                    id = "alert-001",
                    title = "Possible Crocodile Detected",
                    message = "High-confidence detection near the south riverbank. Immediate on-site inspection recommended.",
                    type = AlertType.POSSIBLE_CROCODILE,
                    priority = AlertPriority.CRITICAL,
                    status = AlertStatus.OPEN,
                    createdAt = now,
                    sourceName = "CAM-12 Río Conchal",
                    isRead = false,
                ),
                Alert(
                    id = "alert-002",
                    title = "Motion Detected Near Riverbank",
                    message = "Unusual motion pattern in restricted zone. Flagged for review.",
                    type = AlertType.MOTION_DETECTED,
                    priority = AlertPriority.HIGH,
                    status = AlertStatus.IN_PROGRESS,
                    createdAt = now - 720_000L,                     // 12 min  → today
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
                    createdAt = now - 2_700_000L,                   // 45 min  → today
                    sourceName = "CAM-03 Sector Este",
                    isRead = true,
                ),
                Alert(
                    id = "alert-004",
                    title = "Device Communication Warning",
                    message = "Camera has not reported status in over 2 hours. Check connectivity.",
                    type = AlertType.SYSTEM_WARNING,
                    priority = AlertPriority.MEDIUM,
                    status = AlertStatus.IN_PROGRESS,
                    createdAt = now - 9_000_000L,                   // 2.5 hr  → today
                    sourceName = "CAM-05 Margen Sur",
                    isRead = true,
                ),
                Alert(
                    id = "alert-007",
                    title = "Crocodile Near Access Road",
                    message = "Animal detected crossing the northern access road. Area closed until inspection.",
                    type = AlertType.POSSIBLE_CROCODILE,
                    priority = AlertPriority.CRITICAL,
                    status = AlertStatus.OPEN,
                    createdAt = now - 1 * 24 * 3_600_000L,         // 1 day   → this week
                    sourceName = "CAM-02 Acceso Principal",
                    isRead = false,
                ),
                Alert(
                    id = "alert-008",
                    title = "Large Motion Detected at Pond",
                    message = "Significant movement recorded at the eastern pond perimeter.",
                    type = AlertType.MOTION_DETECTED,
                    priority = AlertPriority.HIGH,
                    status = AlertStatus.IN_PROGRESS,
                    createdAt = now - 2 * 24 * 3_600_000L,         // 2 days  → this week
                    sourceName = "CAM-09 Laguna Este",
                    isRead = true,
                ),
                Alert(
                    id = "alert-009",
                    title = "Camera Alignment Issue",
                    message = "Camera field of view has shifted. Manual recalibration required.",
                    type = AlertType.SYSTEM_WARNING,
                    priority = AlertPriority.MEDIUM,
                    status = AlertStatus.OPEN,
                    createdAt = now - 8 * 24 * 3_600_000L,         // 8 days  → this month
                    sourceName = "CAM-07 Zona Oeste",
                    isRead = true,
                ),
                Alert(
                    id = "alert-005",
                    title = "Battery Low on Remote Camera",
                    message = "Battery level at 8%. Device may go offline within 30 minutes.",
                    type = AlertType.BATTERY_LOW,
                    priority = AlertPriority.LOW,
                    status = AlertStatus.OPEN,
                    createdAt = now - 3 * 24 * 3_600_000L,         // 3 days  → this week
                    sourceName = "CAM-11 Acceso Norte",
                    isRead = true,
                ),
                Alert(
                    id = "alert-006",
                    title = "Sync Completed Successfully",
                    message = "All camera records synchronized. 248 captures uploaded.",
                    type = AlertType.SYNC_COMPLETED,
                    priority = AlertPriority.LOW,
                    status = AlertStatus.CLOSED,
                    createdAt = now - 12 * 24 * 3_600_000L,        // 12 days → this month
                    sourceName = "System",
                    isRead = true,
                ),
            )
        }
}
