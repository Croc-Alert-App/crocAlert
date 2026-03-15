package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CaptureDto(
    val id: String = "",

    // Tal como viene en Firebase
    val createdTime: String? = null,
    val driveId: String = "",
    val driveUrl: String = "",
    val height: Int? = null,
    val width: Int? = null,
    val mimeType: String? = null,
    val name: String = "",
    val size: String? = null,

    // Guardaremos syncedAt como epoch millis para simplificar JSON/Ktor
    val syncedAt: Long? = null
)