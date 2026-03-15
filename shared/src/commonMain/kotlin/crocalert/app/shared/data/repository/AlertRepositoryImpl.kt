package crocalert.app.shared.data.repository

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
import crocalert.app.shared.data.mapper.toDto
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.AlertRemoteDataSource
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.network.NetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AlertRepositoryImpl(
    private val remote: AlertRemoteDataSource
) : AlertRepository {

    private val alertsFlow = MutableStateFlow<List<Alert>>(emptyList())

    override fun observeAlerts(): Flow<List<Alert>> = flow {
        ensureLoaded()
        emitAll(alertsFlow)
    }

    override fun observeAlert(alertId: String): Flow<Alert?> = flow {
        ensureLoaded()
        emitAll(alertsFlow.map { list -> list.firstOrNull { it.id == alertId } })
    }

    override suspend fun createAlert(alert: Alert): String {
        val id = remote.createAlert(alert.toDto()).getOrThrow().id
        refresh()
        return id
    }

    override suspend fun updateAlert(alert: Alert) {
        val id = alert.id.ifBlank { throw IllegalArgumentException("updateAlert necesita id") }
        remote.updateAlert(id, alert.toDto()).getOrThrow()
        refresh()
    }

    override suspend fun deleteAlert(alertId: String) {
        remote.deleteAlert(alertId).getOrThrow()
        refresh()
    }

    // Only fetches on first access; subsequent subscribers reuse cached data
    private suspend fun ensureLoaded() {
        if (alertsFlow.value.isEmpty()) refresh()
    }

    // Failed fetch retains stale data rather than killing observers
    private suspend fun refresh() {
        when (val res = remote.getAlerts()) {
            is ApiResult.Success -> alertsFlow.value = res.data.map { it.toModel() }
            is ApiResult.Error -> { /* stale data retained */ }
        }
    }

    private fun <T> ApiResult<T>.getOrThrow(): T = when (this) {
        is ApiResult.Success -> data
        is ApiResult.Error -> throw NetworkException(message, code)
    }
}