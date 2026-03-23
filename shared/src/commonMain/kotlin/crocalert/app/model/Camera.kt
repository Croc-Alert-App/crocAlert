package crocalert.app.model

import kotlinx.datetime.Instant

data class Camera(
    val id: String = "",
    val name: String = "",
    val isActive: Boolean = true,
    val siteId: String? = null,
    val createdAt: Long? = null,
    val installedAt: Long? = null,
    val expectedImages: Int? = null,
)