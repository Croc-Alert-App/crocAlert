package crocalert.app.shared.data.remote

import crocalert.app.shared.data.dto.CameraDto
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.network.ApiResult

interface CameraRemoteDataSource {
    suspend fun getCameras(): ApiResult<List<CameraDto>>
    suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>>
    suspend fun createCamera(dto: CameraDto): ApiResult<String>
    suspend fun updateCamera(id: String, dto: CameraDto): ApiResult<Unit>
}
