package crocalert.app.shared.data.local

import crocalert.app.shared.data.dto.CameraDto
import kotlinx.coroutines.flow.Flow

interface CameraLocalDataSource {
    fun selectAll(): Flow<List<CameraDto>>
    suspend fun upsertAll(cameras: List<CameraDto>)
    suspend fun lastSyncedAt(): Long?
}
