package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class GlobalDailyCaptureRateDto(
    val date: String,
    val totalCameras: Int,
    val activeCameras: Int,
    val expectedImagesTotal: Int,
    val receivedImagesTotal: Int,
    val missingImagesTotal: Int,
    val extraImagesTotal: Int,
    val captureRate: Double
)