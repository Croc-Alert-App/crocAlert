package crocalert.app.shared.data.remote

import crocalert.app.shared.data.dto.CameraDto
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto
import crocalert.app.shared.network.ApiResult

interface CameraRemoteDataSource {
    suspend fun getCameras(): ApiResult<List<CameraDto>>
    suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>>
    suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto>
    suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>>
    suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto>
    suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto>
    suspend fun createCamera(dto: CameraDto): ApiResult<String>
    suspend fun updateCamera(id: String, dto: CameraDto): ApiResult<Unit>
}
