package crocalert.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.createAlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class DashboardViewModel(
    private val alertRepository: AlertRepository = createAlertRepository(),
    private val loadMetrics: suspend () -> DashboardData = { DashboardMockData.load() },
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.Syncing)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSynced = MutableStateFlow("")
    val lastSynced: StateFlow<String> = _lastSynced.asStateFlow()

    private val _selectedTab = MutableStateFlow(DashboardTab.Home)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun retry() {
        _uiState.value = DashboardUiState.Loading
        _syncStatus.value = SyncStatus.Syncing
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = try {
                val metrics = loadMetrics()

                // Load today's alerts from the real repository
                val tz = TimeZone.currentSystemDefault()
                val todayStartMs = Clock.System.now()
                    .toLocalDateTime(tz)
                    .date
                    .atStartOfDayIn(tz)
                    .toEpochMilliseconds()

                val todayAlerts = alertRepository.observeAlerts().first()
                    .filter { it.createdAt >= todayStartMs }
                    .sortedByDescending { it.createdAt }

                val recentActivity = todayAlerts.map { alert ->
                    ActivityEvent(
                        title = alert.title,
                        timeAgo = formatRelativeTime(alert.createdAt),
                        severity = alert.priority.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        isNew = !alert.isRead,
                        alertId = alert.id,
                    )
                }

                val data = metrics.copy(
                    activeAlerts = todayAlerts.count { it.status == AlertStatus.OPEN },
                    criticalAlerts = todayAlerts.count { it.priority == AlertPriority.CRITICAL },
                    recentActivity = recentActivity,
                )

                _syncStatus.value = SyncStatus.Synced
                val now = Clock.System.now().toLocalDateTime(tz)
                _lastSynced.value = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
                DashboardUiState.Success(data)
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error
                DashboardUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private fun formatRelativeTime(epochMillis: Long): String {
        val diffMs = Clock.System.now().toEpochMilliseconds() - epochMillis
        return when {
            diffMs < 60_000L -> "Ahora mismo"
            diffMs < 3_600_000L -> "Hace ${diffMs / 60_000L} min"
            diffMs < 86_400_000L -> "Hace ${diffMs / 3_600_000L} h"
            else -> "Hace ${diffMs / 86_400_000L} días"
        }
    }
}
