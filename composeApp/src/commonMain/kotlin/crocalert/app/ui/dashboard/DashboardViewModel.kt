package crocalert.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.AlertStatus
import crocalert.app.shared.AppModule
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.sync.SyncPreferencesProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

class DashboardViewModel(
    private val alertRepository: AlertRepository = AppModule.provideAlertRepository(),
    private val cameraRepository: CameraRepository = AppModule.provideCameraRepository(),
    private val syncPrefsProvider: SyncPreferencesProvider = AppModule.provideSyncPreferencesProvider(),
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

    fun setAlertWindowDays(days: Int) {
        viewModelScope.launch {
            syncPrefsProvider.setAlertWindowDays(days)
            loadData()
        }
    }

    fun retry() {
        _syncStatus.value = SyncStatus.Syncing
        viewModelScope.launch {
            alertRepository.refresh()
            cameraRepository.refresh()
        }
        _uiState.value = DashboardUiState.Loading
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val tz = TimeZone.currentSystemDefault()
            val today = Clock.System.todayIn(tz)
            val todayStr = today.toString()

            val todayDashboard = cameraRepository.getMonitoringDashboard(todayStr)

            val activeCameras: Int
            val totalCameras: Int
            val networkHealthPct: Float
            val captureRatePct: Float
            val captureRateLabel: String

            when (todayDashboard) {
                is ApiResult.Success -> {
                    val d = todayDashboard.data
                    activeCameras = d.activeCameras
                    totalCameras = d.totalCameras
                    // Per-camera average capture rate — each camera contributes equally
                    // regardless of expected count, so a Precaución camera at 50% adds 50%,
                    // not 0% as the server's RISK bucket would imply.
                    val activeCams = d.cameras.filter { it.isActive }
                    networkHealthPct = if (activeCams.isNotEmpty())
                        (activeCams.sumOf { it.captureRate } / activeCams.size)
                            .toFloat().coerceIn(0f, 100f)
                    else 0f
                    captureRatePct = d.globalCaptureRate.toFloat()
                    captureRateLabel = "${d.receivedImagesTotal}/${d.expectedImagesTotal}"
                }
                is ApiResult.Error -> {
                    _syncStatus.value = SyncStatus.Error
                    _uiState.value = DashboardUiState.Error(toFriendlyError(todayDashboard.message))
                    return@launch
                }
            }

            val prefs = syncPrefsProvider.preferences.first()
            val windowDays = prefs.alertWindowDays

            val allAlerts = alertRepository.observeAlerts().first()

            val windowStartMs = Clock.System.now()
                .toLocalDateTime(tz)
                .date
                .minus(windowDays - 1, DateTimeUnit.DAY)
                .atStartOfDayIn(tz)
                .toEpochMilliseconds()

            val windowAlerts = allAlerts
                .filter { it.createdAt >= windowStartMs }
                .sortedByDescending { it.createdAt }

            val activeAlertas = windowAlerts.count {
                it.status == AlertStatus.OPEN && it.folder == "alertas"
            }
            val activePreAlertas = windowAlerts.count {
                it.status == AlertStatus.OPEN && it.folder == "pre-alertas"
            }

            val data = DashboardData(
                activeCameras = activeCameras,
                totalCameras = totalCameras,
                networkHealthPct = networkHealthPct,
                activeAlertas = activeAlertas,
                activePreAlertas = activePreAlertas,
                alertWindowDays = windowDays,
                captureRate = captureRateLabel,
                captureRatePct = captureRatePct,
                recentActivity = windowAlerts,
            )

            _syncStatus.value = SyncStatus.Synced
            val now = Clock.System.now().toLocalDateTime(tz)
            _lastSynced.value = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
            _uiState.value = DashboardUiState.Success(data)
        }
    }

    private fun toFriendlyError(raw: String?): String = when {
        raw == null -> "Error desconocido. Intenta de nuevo."
        raw.contains("timeout", ignoreCase = true) ||
            raw.contains("timed out", ignoreCase = true) ->
            "No se pudo conectar con el servidor.\nVerifica tu conexión e intenta de nuevo."
        raw.contains("Network error", ignoreCase = true) ||
            raw.contains("Unable to resolve", ignoreCase = true) ||
            raw.contains("UnknownHost", ignoreCase = true) ->
            "Sin conexión a internet.\nVerifica tu red e intenta de nuevo."
        raw.contains("401") || raw.contains("Unauthorized", ignoreCase = true) ->
            "Sesión expirada. Cierra sesión y vuelve a entrar."
        raw.contains("403") || raw.contains("Forbidden", ignoreCase = true) ->
            "No tienes permiso para ver estos datos."
        raw.contains("500") || raw.contains("502") || raw.contains("503") ->
            "El servidor no está disponible. Intenta más tarde."
        else -> "Error al cargar los datos. Intenta de nuevo."
    }
}
