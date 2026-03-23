package crocalert.app.domain.repository

import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    fun observeCameras(siteId: String? = null): Flow<List<Camera>>
    fun observeCamera(cameraId: String): Flow<Camera?>
    suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>>
    suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto>
    suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>>
    suspend fun createCamera(camera: Camera): String
    suspend fun updateCamera(camera: Camera)
    suspend fun deleteCamera(cameraId: String)
    suspend fun refresh()
}
