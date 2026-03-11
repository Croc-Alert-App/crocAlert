package crocalert.app.feature.alerts.presentation

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * State holder for the Alerts List screen.
 *
 * Designed as a plain Kotlin class so it works across all KMP targets without
 * requiring the Android ViewModel lifecycle. Pass a [CoroutineScope] at
 * construction time; default scope uses [Dispatchers.Main] for production and
 * can be replaced with a [kotlinx.coroutines.test.TestScope] in unit tests.
 *
 * Replacing mock data with real API data:
 * 1. Create a production [AlertRepository] implementation in :shared
 *    (RemoteAlertRepositoryImpl already exists there as a reference).
 * 2. Pass the real implementation to this constructor via DI (Koin module).
 * 3. This class and all UI composables remain unchanged.
 */
class AlertsViewModel(
    private val repository: AlertRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _activeFilter = MutableStateFlow(AlertFilter.ALL)
    val activeFilter: StateFlow<AlertFilter> = _activeFilter.asStateFlow()

    /** Raw sorted list cached after load; filtering is applied client-side. */
    private var allAlerts: List<Alert> = emptyList()
    private var loadJob: Job? = null

    init {
        loadAlerts()
    }

    fun retry() {
        loadAlerts()
    }

    fun setFilter(filter: AlertFilter) {
        _activeFilter.value = filter
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (_activeFilter.value) {
            AlertFilter.ALL -> allAlerts
            AlertFilter.ALERTS -> allAlerts.filter {
                it.priority == AlertPriority.CRITICAL || it.priority == AlertPriority.HIGH
            }
            AlertFilter.PRE_ALERTS -> allAlerts.filter { it.priority == AlertPriority.MEDIUM }
            AlertFilter.INFO -> allAlerts.filter { it.priority == AlertPriority.LOW }
        }
        _uiState.value = if (filtered.isEmpty()) AlertsUiState.Empty else AlertsUiState.Success(filtered)
    }

    private fun loadAlerts() {
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            _uiState.value = AlertsUiState.Loading
            repository.observeAlerts()
                .catch { error ->
                    _uiState.value = AlertsUiState.Error(
                        error.message ?: "Failed to load alerts. Please try again."
                    )
                }
                .collect { alerts ->
                    allAlerts = alerts.sortedByDescending { it.createdAt }
                    applyFilter()
                }
        }
    }

    /** Release the internal scope when the composable leaves composition. */
    fun clear() {
        coroutineScope.cancel()
    }
}
