package crocalert.server.routes

import kotlinx.serialization.Serializable

@Serializable
data class AlertDto(
    val id: String = "",
    val captureId: String = "",
    val createdAt: Long = 0L,
    val status: String = "OPEN",
    val priority: String = "MEDIUM",
    val assignedToUserId: String? = null,
    val closedAt: Long? = null,
    val notes: String? = null,
    val title: String = ""
)