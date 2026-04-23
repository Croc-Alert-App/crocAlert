package crocalert.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.AlertStatus
import crocalert.app.shared.AppModule
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

class DashboardViewModel(
    private val alertRepository: AlertRepository = AppModule.provideAlertRepository(),
    private val cameraRepository: CameraRepository = AppModule.provideCameraRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.Syncing)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSynced = MutableStateFlow("")
    val lastSynced: StateFlow<String> = _lastSynced.asStateFlow()

    private val _selectedTab = MutableStateFlow(DashboardTab.Home)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    private val _activeFilter = MutableStateFlow<DashboardFilter>(DashboardFilter.LastDays(7))
    val activeFilter: StateFlow<DashboardFilter> = _activeFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun selectTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun setFilter(filter: DashboardFilter) {
        _activeFilter.value = filter
        // Only show the overlay when there is already content visible — initial load uses the
        // full DashboardUiState.Loading screen instead.
        if (_uiState.value is DashboardUiState.Success) _isRefreshing.value = true
        loadData()
    }

    fun setCustomRange(startMs: Long, endMs: Long) {
        setFilter(DashboardFilter.Custom(startMs, endMs))
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

            // Build the list of YYYY-MM-DD date strings and alert window bounds from the active filter.
            val filter = _activeFilter.value
            val (dates, windowStartMs, windowEndMs) = when (filter) {
                is DashboardFilter.LastDays -> {
                    val startDay = today.plus(-(filter.days - 1), DateTimeUnit.DAY)
                    val dayList = (0 until filter.days).map { offset ->
                        startDay.plus(offset, DateTimeUnit.DAY).toString()
                    }
                    val startMs = startDay.atStartOfDayIn(tz).toEpochMilliseconds()
                    Triple(dayList, startMs, Clock.System.now().toEpochMilliseconds())
                }
                is DashboardFilter.Custom -> {
                    val startDay = Instant.fromEpochMilliseconds(filter.startMs).toLocalDateTime(tz).date
                    val endDay = Instant.fromEpochMilliseconds(filter.endMs).toLocalDateTime(tz).date
                    val dayList = buildList {
                        var cur = startDay
                        while (cur <= endDay) {
                            add(cur.toString())
                            cur = cur.plus(1, DateTimeUnit.DAY)
                        }
                    }
                    // Extend endMs to cover the full end day.
                    Triple(dayList, filter.startMs, filter.endMs + 86_400_000L - 1)
                }
            }

            // Fetch all days in parallel and aggregate.
            val dashboardResults = dates
                .map { date -> async { cameraRepository.getMonitoringDashboard(date) } }
                .awaitAll()

            val successDays = dashboardResults.mapNotNull { (it as? ApiResult.Success)?.data }

            if (successDays.isEmpty()) {
                val errorMsg = dashboardResults.filterIsInstance<ApiResult.Error>().firstOrNull()?.message
                _syncStatus.value = SyncStatus.Error
                _isRefreshing.value = false
                _uiState.value = DashboardUiState.Error(toFriendlyError(errorMsg))
                return@launch
            }

            val totalCameras = successDays.maxOf { it.totalCameras }
            val activeCameras = successDays.map { it.activeCameras }.average()
                .let { if (it.isNaN()) 0 else it.toInt() }
            val networkHealthPct = successDays.map { day ->
                val active = day.cameras.filter { it.isActive }
                if (active.isNotEmpty()) active.sumOf { it.captureRate } / active.size else 0.0
            }.average().let { if (it.isNaN()) 0f else it.toFloat().coerceIn(0f, 100f) }
            val totalReceived = successDays.sumOf { it.receivedImagesTotal }
            val totalExpected = successDays.sumOf { it.expectedImagesTotal }
            val captureRatePct = if (totalExpected > 0)
                (totalReceived.toFloat() / totalExpected * 100f).coerceIn(0f, 100f)
            else 0f
            val captureRateLabel = "$totalReceived/$totalExpected"

            val allAlerts = alertRepository.observeAlerts().first()

            val windowAlerts = allAlerts
                .filter { it.createdAt in windowStartMs..windowEndMs }
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
                captureRate = captureRateLabel,
                captureRatePct = captureRatePct,
                recentActivity = windowAlerts,
            )

            _syncStatus.value = SyncStatus.Synced
            _isRefreshing.value = false
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
