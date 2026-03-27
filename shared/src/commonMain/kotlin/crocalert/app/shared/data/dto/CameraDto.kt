package crocalert.app.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CameraDto(
    val id: String = "",
    val name: String = "",
    val isActive: Boolean = true,
    val siteId: String? = null,
    val createdAt: Long? = null,
    val installedAt: Long? = null,
    val expectedImages: Int? = null
)