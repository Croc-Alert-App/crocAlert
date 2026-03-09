package crocalert.app.model

data class AlertStatusHistory(
    val id: String = "",
    val alertId: String = "",
    val changedByUserId: String = "",
    val fromStatus: AlertStatus? = null,
    val toStatus: AlertStatus = AlertStatus.OPEN,
    val changedAt: Long = 0L,
    val comment: String? = null,
    val extraJson: String? = null
)