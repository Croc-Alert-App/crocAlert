package crocalert.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.AppModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class DashboardViewModel(
    private val alertRepository: AlertRepository = AppModule.provideAlertRepository(),
    private val cameraRepository: CameraRepository = AppModule.provideCameraRepository(),
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

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun selectTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun retry() {
        _syncStatus.value = SyncStatus.Syncing
        viewModelScope.launch {
            // Global on-demand refresh: full sync for both repos, then reload UI
            alertRepository.refresh()
            cameraRepository.refresh()
        }
        _uiState.value = DashboardUiState.Loading
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val metrics = try {
                loadMetrics()
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error
                _uiState.value = DashboardUiState.Error(e.message ?: "Error desconocido")
                return@launch
            }

            val tz = TimeZone.currentSystemDefault()

            alertRepository.observeAlerts().collect { allAlerts ->
                val todayStartMs = Clock.System.now()
                    .toLocalDateTime(tz)
                    .date
                    .atStartOfDayIn(tz)
                    .toEpochMilliseconds()

                val todayAlerts = allAlerts
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
                _uiState.value = DashboardUiState.Success(data)
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
