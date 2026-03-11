package crocalert.app.model

enum class AlertStatus { OPEN, IN_PROGRESS, CLOSED }
enum class AlertPriority { LOW, MEDIUM, HIGH, CRITICAL }
enum class AlertType {
    MOTION_DETECTED,
    IMAGE_UPLOADED,
    SYSTEM_WARNING,
    POSSIBLE_CROCODILE,
    BATTERY_LOW,
    SYNC_COMPLETED,
    UNKNOWN,
}
enum class ContactPointType { EMAIL, SMS, WHATSAPP, PUSH }
enum class NotificationChannel { EMAIL, SMS, WHATSAPP, PUSH }
enum class DeliveryStatus { PENDING, SENT, FAILED }