package crocalert.app.shared.data.mapper

import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.data.dto.AlertDto

private fun statusFromString(value: String): AlertStatus =
    runCatching { AlertStatus.valueOf(value) }.getOrElse { AlertStatus.OPEN }

private fun priorityFromString(value: String): AlertPriority =
    runCatching { AlertPriority.valueOf(value) }.getOrElse { AlertPriority.MEDIUM }

fun AlertDto.toModel(): Alert = Alert(
    id = id,
    captureId = captureId,
    createdAt = createdAt,
    status = runCatching { AlertStatus.valueOf(status) }.getOrDefault(AlertStatus.OPEN),
    priority = runCatching { AlertPriority.valueOf(priority) }.getOrDefault(AlertPriority.MEDIUM),
    assignedToUserId = assignedToUserId,
    closedAt = closedAt,
    notes = notes,
    title = title
)

fun Alert.toDto(): AlertDto = AlertDto(
    id = id,
    captureId = captureId,
    createdAt = createdAt,
    status = status.name,
    priority = priority.name,
    assignedToUserId = assignedToUserId,
    closedAt = closedAt,
    notes = notes,
    title = title
)