package crocalert.app.domain.repository

import crocalert.app.model.Alert
import crocalert.app.model.Camera
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    fun observeCameras(siteId: String? = null): Flow<List<Camera>>
    fun observeCamera(cameraId: String): Flow<Camera?>

    suspend fun createCamera(camera: Camera): String
    suspend fun updateCamera(camera: Camera)
    suspend fun deleteCamera(cameraId: String)
}