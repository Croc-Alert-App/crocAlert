package crocalert.app.shared.data.mapper

import crocalert.app.model.Site
import crocalert.app.shared.data.dto.SiteDto

/** Maps [SiteDto] to the [Site] domain model. */
fun SiteDto.toModel(): Site = Site(
    id = id,
    code = code,
    name = name,
    description = description,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    centerLat = centerLat,
    centerLng = centerLng,
    region = region
)
