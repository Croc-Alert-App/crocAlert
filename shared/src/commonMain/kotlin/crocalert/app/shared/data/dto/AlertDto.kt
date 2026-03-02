package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlertDto(
    val id: String,
    val captureId: String,
    val createdAt: String,          // ISO-8601
    val status: String,
    val priority: String,
    val assignedToUserId: String? = null,
    val closedAt: String? = null,   // ISO-8601
    val notes: String? = null,
    val title: String
)