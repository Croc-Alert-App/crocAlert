package crocalert.app.shared.data.repository

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
import crocalert.app.shared.data.mapper.toDto
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.data.remote.AlertRemoteDataSource
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AlertRepositoryImpl(
    private val remote: AlertRemoteDataSource
) : AlertRepository {

    // null = not yet loaded; empty list = server returned zero alerts (valid state)
    private val alertsFlow = MutableStateFlow<List<Alert>?>(null)

    private val _lastRefreshError = MutableStateFlow<String?>(null)
    override val lastRefreshError: StateFlow<String?> = _lastRefreshError.asStateFlow()

    override fun observeAlerts(): Flow<List<Alert>> = flow {
        ensureLoaded()
        emitAll(alertsFlow.map { it ?: emptyList() })
    }

    override fun observeAlert(alertId: String): Flow<Alert?> = flow {
        ensureLoaded()
        emitAll(alertsFlow.map { list -> list?.firstOrNull { it.id == alertId } })
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

    private suspend fun ensureLoaded() {
        if (alertsFlow.value == null) refresh()
    }

    // Retains stale data on error to avoid wiping the observer cache.
    // Signals the error via lastRefreshError so the UI can show a sync warning.
    private suspend fun refresh() {
        when (val res = remote.getAlerts()) {
            is ApiResult.Success -> {
                alertsFlow.value = res.data.map { it.toModel() }
                _lastRefreshError.value = null
            }
            is ApiResult.Error -> _lastRefreshError.value = res.message
        }
    }

    private fun <T> ApiResult<T>.getOrThrow(): T = when (this) {
        is ApiResult.Success -> data
        is ApiResult.Error -> error(message)
    }
}
