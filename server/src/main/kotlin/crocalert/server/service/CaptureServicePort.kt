package crocalert.server.service

import crocalert.app.shared.data.dto.CaptureDto

interface CaptureServicePort {
    suspend fun getAll(): List<CaptureDto>
    suspend fun getById(id: String): CaptureDto?
    suspend fun getByCameraId(cameraId: String): List<CaptureDto>
    suspend fun getByFolder(folder: String): List<CaptureDto>
    suspend fun create(dto: CaptureDto): String
    suspend fun update(id: String, dto: CaptureDto): Boolean
    suspend fun delete(id: String): Boolean
}
