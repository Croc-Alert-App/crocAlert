package crocalert.app.shared.data.mapper

import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.model.AlertType
import crocalert.app.shared.data.dto.AlertDto

private inline fun <reified T : Enum<T>> enumFromStringOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrElse {
        println("AlertMapper: unknown ${T::class.simpleName} '$value', defaulting to $default")
        default
    }

private fun alertTypeFromFolder(folder: String?): AlertType = when (folder?.lowercase()) {
    "alertas" -> AlertType.POSSIBLE_CROCODILE
    else -> AlertType.IMAGE_UPLOADED
}

fun AlertDto.toModel(): Alert = Alert(
    id = id,
    captureId = captureId,
    cameraId = cameraId,
    aiConfidence = aiConfidence,
    createdAt = createdAt,
    status = enumFromStringOrDefault(status, AlertStatus.OPEN),
    priority = enumFromStringOrDefault(priority, AlertPriority.MEDIUM),
    assignedToUserId = assignedToUserId,
    closedAt = closedAt,
    notes = notes,
    title = title,
    thumbnailUrl = thumbnailUrl,
    folder = folder,
    type = alertTypeFromFolder(folder),
)

fun Alert.toDto(): AlertDto = AlertDto(
    id = id,
    captureId = captureId,
    cameraId = cameraId,
    aiConfidence = aiConfidence,
    createdAt = createdAt,
    status = status.name,
    priority = priority.name,
    assignedToUserId = assignedToUserId,
    closedAt = closedAt,
    notes = notes,
    title = title,
    thumbnailUrl = thumbnailUrl,
    folder = folder,
)