package crocalert.app.shared.data.repository

import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.data.local.CameraLocalDataSource
import crocalert.app.shared.data.mapper.toDto
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.CameraRemoteDataSource
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.sync.SyncPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

class CameraRepositoryImpl(
    private val remote: CameraRemoteDataSource,
    private val local: CameraLocalDataSource,
    private val syncPrefs: Flow<SyncPreferences> = kotlinx.coroutines.flow.flowOf(SyncPreferences()),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : CameraRepository {

    override fun observeCameras(siteId: String?): Flow<List<Camera>> =
        local.selectAll()
            .map { dtos ->
                val all = dtos.map { it.toModel() }
                if (siteId != null) all.filter { it.siteId == siteId } else all
            }
            .also { coroutineScope.launch { syncIfStale() } }

    override fun observeCamera(cameraId: String): Flow<Camera?> =
        local.selectAll()
            .map { dtos -> dtos.firstOrNull { it.id == cameraId }?.toModel() }
            .also { coroutineScope.launch { syncIfStale() } }

    override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> =
        remote.getCapturesByCamera(cameraId)

    override suspend fun createCamera(camera: Camera): String {
        val dto = camera.toDto()
        return when (val result = remote.createCamera(dto)) {
            is ApiResult.Success -> { sync(); result.data }
            is ApiResult.Error   -> throw Exception(result.message)
        }
    }

    override suspend fun updateCamera(camera: Camera) {
        val dto = camera.toDto()
        when (val result = remote.updateCamera(camera.id, dto)) {
            is ApiResult.Success -> sync()
            is ApiResult.Error   -> throw Exception(result.message)
        }
    }

    override suspend fun deleteCamera(cameraId: String) {
        val existing = local.selectAll().first().firstOrNull { it.id == cameraId }
            ?: throw Exception("Camera not found: $cameraId")
        val deactivated = existing.copy(isActive = false)
        when (val result = remote.updateCamera(cameraId, deactivated)) {
            is ApiResult.Success -> sync()
            is ApiResult.Error   -> throw Exception(result.message)
        }
    }

    override suspend fun refresh() = sync()

    internal suspend fun forceSync() = sync()

    private suspend fun syncIfStale() {
        val ttl = syncPrefs.first().camerasTtlMinutes
        val staleThreshold = Clock.System.now().minus(ttl.minutes).toEpochMilliseconds()
        val lastSync = local.lastSyncedAt() ?: 0L
        if (lastSync < staleThreshold) sync()
    }

    private suspend fun sync() {
        when (val res = remote.getCameras()) {
            is ApiResult.Success -> local.upsertAll(res.data)
            is ApiResult.Error -> { /* stale data retained */ }
        }
    }
}
