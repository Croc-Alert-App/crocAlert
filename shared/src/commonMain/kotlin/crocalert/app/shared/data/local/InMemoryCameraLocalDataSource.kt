package crocalert.app.shared.data.local

import crocalert.app.shared.data.dto.CameraDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

class InMemoryCameraLocalDataSource : CameraLocalDataSource {

    private val _cameras = MutableStateFlow<List<CameraDto>>(emptyList())
    private var _lastSyncedAt: Long? = null

    override fun selectAll(): Flow<List<CameraDto>> = _cameras

    override suspend fun upsertAll(cameras: List<CameraDto>) {
        _cameras.value = cameras
        _lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun lastSyncedAt(): Long? = _lastSyncedAt
}
