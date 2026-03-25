package crocalert.server.service

import crocalert.app.shared.data.dto.SiteDto

interface SiteServicePort {
    suspend fun getAll(): List<SiteDto>
    suspend fun getById(id: String): SiteDto?
}
