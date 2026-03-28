package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CameraMonitoringDashboardDto(
    val date: String,
    val totalCameras: Int,
    val activeCameras: Int,

    val expectedImagesTotal: Int,
    val receivedImagesTotal: Int,
    val missingImagesTotal: Int,
    val extraImagesTotal: Int,
    val globalCaptureRate: Double,

    val healthyCameras: Int,
    val cautionCameras: Int,
    val riskCameras: Int,
    val healthyRate: Double,
    val operationalRate: Double,

    val cameras: List<CameraHealthCheckDto>
)