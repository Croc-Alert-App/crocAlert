package crocalert.app.shared.data.repository

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
import crocalert.app.shared.data.mapper.toDto
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.AlertRemoteDataSource
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AlertRepositoryImpl(
    private val remote: AlertRemoteDataSource
) : AlertRepository {

    private val alertsFlow = MutableStateFlow<List<Alert>>(emptyList())

    // ✅ ahora carga del server apenas alguien observa
    override fun observeAlerts(): Flow<List<Alert>> = flow {
        refresh()
        emitAll(alertsFlow)
    }

    override fun observeAlert(alertId: String): Flow<Alert?> =
        alertsFlow.map { list -> list.firstOrNull { it.id == alertId } }

    override suspend fun createAlert(alert: Alert): String {
        val dto = alert.toDto()
        return when (val res = remote.createAlert(dto)) {
            is ApiResult.Success -> {
                refresh()
                res.data
            }
            is ApiResult.Error -> error(res.message)
        }
    }

    override suspend fun updateAlert(alert: Alert) {
        val id = alert.id.ifBlank { error("updateAlert necesita id") }
        val dto = alert.toDto()
        when (val res = remote.updateAlert(id, dto)) {
            is ApiResult.Success -> refresh()
            is ApiResult.Error -> error(res.message)
        }
    }

    override suspend fun deleteAlert(alertId: String) {
        when (val res = remote.deleteAlert(alertId)) {
            is ApiResult.Success -> refresh()
            is ApiResult.Error -> error(res.message)
        }
    }

    private suspend fun refresh() {
        when (val res = remote.getAlerts()) {
            is ApiResult.Success -> alertsFlow.value = res.data.map { it.toModel() }
            is ApiResult.Error -> error(res.message)
        }
    }
}