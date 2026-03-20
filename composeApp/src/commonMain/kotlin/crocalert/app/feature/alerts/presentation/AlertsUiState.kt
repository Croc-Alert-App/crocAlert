package crocalert.app.feature.alerts.presentation

import crocalert.app.model.Alert

/**
 * Represents the complete UI state of the Alerts List screen.
 *
 * - [Loading]  : initial load or manual refresh in progress
 * - [Success]  : at least one alert matched the active filter
 * - [Empty]    : load succeeded but the filter returned no results; carries a [message]
 * - [Error]    : load failed; includes a human-readable [message] and a retry action
 */
sealed class AlertsUiState {
    data object Loading : AlertsUiState()
    data class Success(val alerts: List<Alert>) : AlertsUiState()
    data class Empty(val message: String = "No se encontraron alertas.") : AlertsUiState()
    data class Error(val message: String) : AlertsUiState()
}

/**
 * Date-range filter applied client-side on top of the raw alert list.
 * CUSTOM is a placeholder — a date-picker implementation can replace it
 * without touching the ViewModel contract or the UI layer.
 */
enum class AlertFilter(val label: String) {
    ALL("Todas"),
    TODAY("Hoy"),
    THIS_WEEK("Esta semana"),
    THIS_MONTH("Este mes"),
    CUSTOM("Personalizado"),
}

/** Controls the chronological order of the displayed alert list. */
enum class SortDirection(val label: String) {
    DESC("↓ Más reciente"),
    ASC("↑ Más antiguo"),
}

/**
 * A confirmed custom date range for [AlertFilter.CUSTOM].
 * Stored in the ViewModel so the UI can display the selected dates in the chip label.
 */
data class DateRange(val startMs: Long, val endMs: Long)
