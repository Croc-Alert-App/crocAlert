package crocalert.server.service

import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraDto
import crocalert.app.shared.data.dto.CameraHealthCheckDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto

interface CameraServicePort {
    suspend fun getAll(): List<CameraDto>
    suspend fun getById(id: String): CameraDto?
    suspend fun create(dto: CameraDto): String
    suspend fun update(id: String, dto: CameraDto): Boolean
    suspend fun delete(id: String): Boolean
    suspend fun getDailyStats(cameraId: String, date: String): CameraDailyStatsDto?
    suspend fun getDailyStatsForAll(date: String): List<CameraDailyStatsDto>
    suspend fun getGlobalDailyCaptureRate(date: String): GlobalDailyCaptureRateDto
    suspend fun getCameraHealthCheck(cameraId: String, date: String): CameraHealthCheckDto?
    suspend fun getAllCameraHealthChecks(date: String): List<CameraHealthCheckDto>
    suspend fun getMonitoringDashboard(date: String): CameraMonitoringDashboardDto
}
