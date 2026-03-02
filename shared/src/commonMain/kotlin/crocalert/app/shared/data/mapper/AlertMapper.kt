package crocalert.app.shared.data.mapper

import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.data.dto.AlertDto
import kotlinx.datetime.Instant

private fun statusFromString(value: String): AlertStatus =
    runCatching { AlertStatus.valueOf(value) }.getOrElse { AlertStatus.OPEN }

private fun priorityFromString(value: String): AlertPriority =
    runCatching { AlertPriority.valueOf(value) }.getOrElse { AlertPriority.MEDIUM }

fun AlertDto.toModel(): Alert = Alert(
    id = id,
    captureId = captureId,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
    status = statusFromString(status),
    priority = priorityFromString(priority),
    assignedToUserId = assignedToUserId,
    closedAt = closedAt?.let { Instant.parse(it).toEpochMilliseconds() },
    notes = notes,
    title = title
)

fun Alert.toDto(): AlertDto = AlertDto(
    id = id,
    captureId = captureId,
    createdAt = Instant.fromEpochMilliseconds(createdAt).toString(),
    status = status.name,
    priority = priority.name,
    assignedToUserId = assignedToUserId,
    closedAt = closedAt?.let { Instant.fromEpochMilliseconds(it).toString() },
    notes = notes,
    title = title
)