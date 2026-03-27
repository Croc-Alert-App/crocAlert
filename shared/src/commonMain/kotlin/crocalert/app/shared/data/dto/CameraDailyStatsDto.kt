package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CameraDailyStatsDto(
    val cameraId: String,
    val date: String,
    val expectedImages: Int,
    val receivedImages: Int,
    val missingImages: Int,
    val isActive: Boolean,
    val installedAt: Long? = null
)