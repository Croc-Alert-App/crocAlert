package crocalert.app.shared.data.remote

import crocalert.app.shared.data.dto.SiteDto
import crocalert.app.shared.network.ApiResult

interface SiteRemoteDataSource {
    suspend fun getSites(): ApiResult<List<SiteDto>>
}
