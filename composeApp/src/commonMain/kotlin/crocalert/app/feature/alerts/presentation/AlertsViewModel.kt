package crocalert.app.feature.alerts.presentation

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * State holder for the Alerts List screen.
 *
 * Single responsibility: owns the reactive state of the screen
 * (loading, error, filter, sort, custom range).
 *
 * Replacing mock data with real API data:
 * 1. Create a production [AlertRepository] in :shared.
 * 2. Provide it via Koin. No changes to this class or the UI are needed.
 */
class AlertsViewModel(
    private val repository: AlertRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) {
    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _activeFilter = MutableStateFlow(AlertFilter.ALL)
    val activeFilter: StateFlow<AlertFilter> = _activeFilter.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.DESC)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    private val _customRange = MutableStateFlow<DateRange?>(null)
    val customRange: StateFlow<DateRange?> = _customRange.asStateFlow()

    private var rawAlerts: List<Alert> = emptyList()
    private var loadJob: Job? = null

    init { loadAlerts() }

    fun retry() = loadAlerts()

    /** Placeholder for future Firebase sync — currently reloads from the repository. */
    fun refresh() = loadAlerts()

    fun setFilter(filter: AlertFilter) {
        _activeFilter.value = filter
        applyFilterAndSort()
    }

    fun toggleSort() {
        _sortDirection.value =
            if (_sortDirection.value == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
        applyFilterAndSort()
    }

    /** Activates the CUSTOM filter with the given date range and persists it for the UI chip label. */
    fun setCustomRange(startMs: Long, endMs: Long) {
        _customRange.value = DateRange(startMs, endMs)
        _activeFilter.value = AlertFilter.CUSTOM
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val now = Clock.System.now().toEpochMilliseconds()

        val filtered = when (_activeFilter.value) {
            AlertFilter.ALL -> rawAlerts
            AlertFilter.TODAY -> rawAlerts.filter { it.createdAt >= startOfTodayMs() }
            AlertFilter.THIS_WEEK -> rawAlerts.filter { it.createdAt >= now - SEVEN_DAYS_MS }
            AlertFilter.THIS_MONTH -> rawAlerts.filter { it.createdAt >= now - THIRTY_DAYS_MS }
            AlertFilter.CUSTOM -> {
                val range = _customRange.value
                if (range == null) {
                    _uiState.value = AlertsUiState.Empty("Tap 'Custom' to select a date range.")
                    return
                }
                // Include the full end-day by extending the end timestamp to 23:59:59.999.
                rawAlerts.filter { it.createdAt in range.startMs..(range.endMs + DAY_MS - 1L) }
            }
        }

        val sorted = when (_sortDirection.value) {
            SortDirection.DESC -> filtered.sortedByDescending { it.createdAt }
            SortDirection.ASC -> filtered.sortedBy { it.createdAt }
        }

        _uiState.value = if (sorted.isEmpty()) AlertsUiState.Empty() else AlertsUiState.Success(sorted)
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
                    rawAlerts = alerts
                    applyFilterAndSort()
                }
        }
    }

    private fun startOfTodayMs(): Long {
        val tz = TimeZone.currentSystemDefault()
        return Clock.System.now().toLocalDateTime(tz).date.atStartOfDayIn(tz).toEpochMilliseconds()
    }

    fun clear() = coroutineScope.cancel()

    private companion object {
        const val DAY_MS = 24 * 3_600_000L
        const val SEVEN_DAYS_MS = 7 * DAY_MS
        const val THIRTY_DAYS_MS = 30 * DAY_MS
    }
}
