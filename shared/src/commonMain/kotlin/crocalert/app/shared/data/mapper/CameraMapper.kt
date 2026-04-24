package crocalert.app.shared.data.mapper

import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDto

/** Maps [CameraDto] to the [Camera] domain model. */
fun CameraDto.toModel(): Camera = Camera(
    id = id,
    name = name,
    isActive = isActive,
    siteId = siteId,
    createdAt = createdAt,
    installedAt = installedAt,
    expectedImages = expectedImages,
)

/** Maps [Camera] domain model to [CameraDto] for server transmission. */
fun Camera.toDto(): CameraDto = CameraDto(
    id = id,
    name = name,
    isActive = isActive,
    siteId = siteId,
    createdAt = createdAt,
    installedAt = installedAt,
    expectedImages = expectedImages,
)
