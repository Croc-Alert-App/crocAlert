package crocalert.app.model

data class Alert(
    val id: String = "",
    val captureId: String = "",
    val createdAt: Long = 0L,
    val status: AlertStatus = AlertStatus.OPEN,
    val priority: AlertPriority = AlertPriority.MEDIUM,
    val assignedToUserId: String? = null,
    val closedAt: Long? = null,
    val notes: String? = null,
    val title: String = "",
    // Fields added for the Alerts List feature
    val message: String = "",
    val type: AlertType = AlertType.UNKNOWN,
    val sourceName: String = "",
    val thumbnailUrl: String? = null,
    val isRead: Boolean = false,
)