package crocalert.app.shared.data.repository

import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.CameraRemoteDataSource
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class CameraRepositoryImpl(
    private val remote: CameraRemoteDataSource
) : CameraRepository {

    // null = not yet loaded; empty list = server returned zero cameras (valid state)
    private val camerasFlow = MutableStateFlow<List<Camera>?>(null)

    override fun observeCameras(siteId: String?): Flow<List<Camera>> = flow {
        ensureLoaded()
        emitAll(camerasFlow.map { cameras ->
            val all = cameras ?: emptyList()
            if (siteId != null) all.filter { it.siteId == siteId } else all
        })
    }

    override fun observeCamera(cameraId: String): Flow<Camera?> = flow {
        ensureLoaded()
        emitAll(camerasFlow.map { list -> list?.firstOrNull { it.id == cameraId } })
    }

    override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> =
        remote.getCapturesByCamera(cameraId)

    override suspend fun createCamera(camera: Camera): String = error("not implemented")
    override suspend fun updateCamera(camera: Camera) = error("not implemented")
    override suspend fun deleteCamera(cameraId: String) = error("not implemented")

    private suspend fun ensureLoaded() {
        if (camerasFlow.value == null) refresh()
    }

    // Silently retains stale data on error to avoid killing observers.
    private suspend fun refresh() {
        when (val res = remote.getCameras()) {
            is ApiResult.Success -> camerasFlow.value = res.data.map { it.toModel() }
            is ApiResult.Error -> { /* stale data retained */ }
        }
    }
}
