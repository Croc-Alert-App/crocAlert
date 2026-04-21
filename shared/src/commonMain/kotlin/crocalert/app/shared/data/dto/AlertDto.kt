package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlertDto(
    val id: String = "",
    val captureId: String = "",
    val cameraId: String = "",
    val aiConfidence: Float? = null,
    val createdAt: Long = 0L,
    val status: String = "OPEN",
    val priority: String = "MEDIUM",
    val assignedToUserId: String? = null,
    val closedAt: Long? = null,
    val notes: String? = null,
    val thumbnailUrl: String? = null,
    val title: String = "",
    val folder: String? = null,
)
