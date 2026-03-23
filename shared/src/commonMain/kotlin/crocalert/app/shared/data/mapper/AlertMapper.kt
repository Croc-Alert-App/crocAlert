package crocalert.app.shared.data.mapper

import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.data.dto.AlertDto

private inline fun <reified T : Enum<T>> enumFromStringOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrElse {
        println("AlertMapper: unknown ${T::class.simpleName} '$value', defaulting to $default")
        default
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
    folder = folder,
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
    folder = folder,
)