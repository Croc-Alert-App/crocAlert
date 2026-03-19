package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CaptureDto(
    val id: String = "",
    val cameraId: String = "",
    val cameraRef: String? = null,
    val captureTime: Long? = null,
    val createdTime: String? = null,
    val driveId: String = "",
    val driveUrl: String = "",
    val folder: String? = null,
    val height: Int? = null,
    val width: Int? = null,
    val mimeType: String? = null,
    val name: String = "",
    val size: String? = null,
    val syncedAt: Long? = null
)