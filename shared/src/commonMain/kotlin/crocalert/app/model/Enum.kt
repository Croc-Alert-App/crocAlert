package crocalert.app.model



enum class AlertStatus { OPEN, IN_PROGRESS, CLOSED }
enum class AlertPriority { LOW, MEDIUM, HIGH, CRITICAL }
enum class ContactPointType { EMAIL, SMS, WHATSAPP, PUSH }
enum class NotificationChannel { EMAIL, SMS, WHATSAPP, PUSH }
enum class DeliveryStatus { PENDING, SENT, FAILED }