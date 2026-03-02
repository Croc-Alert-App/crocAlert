package crocalert.app.model

import kotlinx.datetime.Instant

data class Camera(
    val cameraId: String = "",
    val siteId: String = "",
    val code: String = "",
    val name: String = "",
    val isActive: Boolean = true,
    val installedAt: Instant? = null,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)