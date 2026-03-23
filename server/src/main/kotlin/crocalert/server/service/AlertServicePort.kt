package crocalert.server.service

import crocalert.app.shared.data.dto.AlertDto

interface AlertServicePort {
    suspend fun getAll(since: Long? = null): List<AlertDto>
    suspend fun getById(id: String): AlertDto?
    suspend fun create(dto: AlertDto): String
    suspend fun update(id: String, dto: AlertDto): Boolean
    suspend fun delete(id: String): Boolean
}
