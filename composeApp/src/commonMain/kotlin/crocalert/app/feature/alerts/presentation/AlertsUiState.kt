package crocalert.app.feature.alerts.presentation

import crocalert.app.model.Alert

/**
 * Represents the complete UI state of the Alerts List screen.
 *
 * - [Loading]  : initial load or retry in progress
 * - [Success]  : at least one alert is available
 * - [Empty]    : load succeeded but the list is empty (or the active filter yields no results)
 * - [Error]    : load failed; includes a human-readable message and a retry action
 */
sealed class AlertsUiState {
    data object Loading : AlertsUiState()
    data class Success(val alerts: List<Alert>) : AlertsUiState()
    data object Empty : AlertsUiState()
    data class Error(val message: String) : AlertsUiState()
}

/**
 * Client-side filter applied on top of the raw alert list.
 * The ViewModel holds the active filter and reapplies it whenever raw data changes,
 * so adding server-side filtering later is a non-breaking change.
 */
enum class AlertFilter(val label: String) {
    ALL("All"),
    ALERTS("Alerts"),
    PRE_ALERTS("Pre-Alerts"),
    INFO("Info"),
}
