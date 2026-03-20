package crocalert.app.feature.alerts.data

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf

/**
 * Phase 1 mock implementation of [AlertRepository].
 *
 * Returns hardcoded data from [AlertSampleData] via a [MutableStateFlow].
 * Using a StateFlow (rather than flowOf) means the stream stays open for
 * future updates — e.g. a test can push a new list to simulate live updates.
 *
 * To replace with a real remote source:
 * 1. Create `RemoteAlertRepository(private val dataSource: AlertRemoteDataSource)`
 *    in the `:shared` module (an implementation already exists there).
 * 2. Register it in your DI module (Koin) instead of this class.
 * 3. No changes to the ViewModel or UI layer are required.
 */
class MockAlertRepository : AlertRepository {

    private val alertsFlow = MutableStateFlow(AlertSampleData.alerts)

    override val lastRefreshError: Flow<String?> = flowOf(null)

    override fun observeAlerts(): Flow<List<Alert>> = alertsFlow

    override fun observeAlert(alertId: String): Flow<Alert?> =
        alertsFlow.map { list -> list.find { it.id == alertId } }

    override suspend fun createAlert(alert: Alert): String {
        alertsFlow.value = alertsFlow.value + alert
        return alert.id
    }

    override suspend fun updateAlert(alert: Alert) {
        alertsFlow.value = alertsFlow.value.map { if (it.id == alert.id) alert else it }
    }

    override suspend fun deleteAlert(alertId: String) {
        alertsFlow.value = alertsFlow.value.filter { it.id != alertId }
    }
}
