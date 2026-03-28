package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CameraHealthCheckDto(
    val cameraId: String,
    val date: String,
    val expectedImages: Int,
    val receivedImages: Int,
    val missingImages: Int,
    val extraImages: Int,
    val captureRate: Double,
    val healthStatus: String,
    val isActive: Boolean,
    val installedAt: Long? = null
)