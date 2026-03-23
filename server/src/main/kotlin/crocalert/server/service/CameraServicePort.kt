package crocalert.server.service

import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraDto

interface CameraServicePort {
    suspend fun getAll(): List<CameraDto>
    suspend fun getById(id: String): CameraDto?
    suspend fun create(dto: CameraDto): String
    suspend fun update(id: String, dto: CameraDto): Boolean
    suspend fun delete(id: String): Boolean
    suspend fun getDailyStats(cameraId: String, date: String): CameraDailyStatsDto?
    suspend fun getDailyStatsForAll(date: String): List<CameraDailyStatsDto>
}
