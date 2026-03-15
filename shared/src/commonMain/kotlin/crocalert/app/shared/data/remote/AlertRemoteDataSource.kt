package crocalert.app.shared.data.remote

import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.network.ApiResult

interface AlertRemoteDataSource {

    suspend fun getAlerts(): ApiResult<List<AlertDto>>

    suspend fun getAlert(id: String): ApiResult<AlertDto>

    suspend fun createAlert(dto: AlertDto): ApiResult<String>

    suspend fun updateAlert(id: String, dto: AlertDto): ApiResult<Unit>

    suspend fun deleteAlert(id: String): ApiResult<Unit>
}