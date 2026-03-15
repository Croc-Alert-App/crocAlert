package crocalert.app.model


data class Notification(
    val id: String = "",
    val alertId: String = "",
    val contactPointId: String? = null,
    val channel: NotificationChannel = NotificationChannel.PUSH,
    val recipient: String = "",
    val payloadJson: String? = null,
    val sentAt: Long? = null,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.PENDING,
    val failureReason: String? = null
)