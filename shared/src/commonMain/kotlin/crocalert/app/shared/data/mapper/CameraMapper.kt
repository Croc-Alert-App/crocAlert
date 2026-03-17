package crocalert.app.shared.data.mapper

import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDto

fun CameraDto.toModel(): Camera = Camera(
    id = id,
    name = name,
    isActive = isActive,
    siteId = siteId,
    createdAt = createdAt,
    installedAt = installedAt
)
