package crocalert.server.service

import crocalert.server.dto.AlertDto
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AlertService {
    private val alerts = ConcurrentHashMap<String, AlertDto>()

    suspend fun getAll(): List<AlertDto> = alerts.values.toList()

    suspend fun create(dto: AlertDto): String {
        val id = dto.id.ifBlank { UUID.randomUUID().toString() }
        alerts[id] = dto.copy(id = id)
        return id
    }
}