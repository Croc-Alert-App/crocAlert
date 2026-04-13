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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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

    // Trend range: end date (inclusive). Defaults to today; the chart always shows 7 days back.
    private val _trendEndMs = MutableStateFlow<Long?>(null)
    val trendEndMs: StateFlow<Long?> = _trendEndMs.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun selectTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun setTrendEndDate(endMs: Long) {
        _trendEndMs.value = endMs
        _uiState.value = DashboardUiState.Loading
        loadData()
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

            // Resolve trend end date (today if no custom selection)
            val trendEndDate: LocalDate = _trendEndMs.value
                ?.let {
                    Clock.System.now()
                        .also { _ -> }
                    // Convert millis to LocalDate
                    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(it)
                    instant.toLocalDateTime(tz).date
                } ?: today

            // ── Fetch dashboard for today (KPIs) and for 7-day trend in parallel ──
            val todayStr = today.toString()
            val trendDates = (6 downTo 0).map { offset ->
                trendEndDate.minus(offset, DateTimeUnit.DAY)
            }

            val todayDashboardDeferred = async {
                cameraRepository.getMonitoringDashboard(todayStr)
            }
            val trendDeferreds = trendDates.map { date ->
                async { cameraRepository.getMonitoringDashboard(date.toString()) }
            }

            val todayDashboard = todayDashboardDeferred.await()
            val trendResults = trendDeferreds.map { it.await() }

            // ── Build KPI base from dashboard ──
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
                    networkHealthPct = d.healthyRate.toFloat()
                    captureRatePct = d.globalCaptureRate.toFloat()
                    captureRateLabel = "${d.receivedImagesTotal}/${d.expectedImagesTotal}"
                }
                is ApiResult.Error -> {
                    _syncStatus.value = SyncStatus.Error
                    _uiState.value = DashboardUiState.Error(toFriendlyError(todayDashboard.message))
                    return@launch
                }
            }

            // ── Build 7-day trend ──
            val networkTrend = trendDates.mapIndexed { index, date ->
                val healthRate = when (val res = trendResults[index]) {
                    is ApiResult.Success -> res.data.healthyRate.toFloat()
                    is ApiResult.Error -> 0f
                }
                val dayLabel = dayAbbreviation(date.dayOfWeek)
                NetworkTrendDay(
                    label = dayLabel,
                    value = healthRate,
                    isToday = date == today
                )
            }

            // ── Alert window from preferences ──
            val prefs = syncPrefsProvider.preferences.first()
            val windowDays = prefs.alertWindowDays

            // ── Snapshot of current alerts (first() avoids an infinite collect that freezes the UI) ──
            val allAlerts = alertRepository.observeAlerts().first()

            val windowStartMs = Clock.System.now()
                .toLocalDateTime(tz)
                .date
                .minus(windowDays - 1, DateTimeUnit.DAY)
                .atStartOfDayIn(tz)
                .toEpochMilliseconds()

            val todayStartMs = today
                .atStartOfDayIn(tz)
                .toEpochMilliseconds()

            val windowAlerts = allAlerts
                .filter { it.createdAt >= windowStartMs }
                .sortedByDescending { it.createdAt }

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

            val data = DashboardData(
                activeCameras = activeCameras,
                totalCameras = totalCameras,
                networkHealthPct = networkHealthPct,
                activeAlerts = windowAlerts.count { it.status == AlertStatus.OPEN },
                alertWindowDays = windowDays,
                captureRate = captureRateLabel,
                captureRatePct = captureRatePct,
                networkTrend = networkTrend,
                recentActivity = recentActivity,
            )

            _syncStatus.value = SyncStatus.Synced
            val now = Clock.System.now().toLocalDateTime(tz)
            _lastSynced.value = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
            _uiState.value = DashboardUiState.Success(data)
        }
    }

    private fun dayAbbreviation(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.MONDAY    -> "L"
        DayOfWeek.TUESDAY   -> "M"
        DayOfWeek.WEDNESDAY -> "X"
        DayOfWeek.THURSDAY  -> "J"
        DayOfWeek.FRIDAY    -> "V"
        DayOfWeek.SATURDAY  -> "S"
        DayOfWeek.SUNDAY    -> "D"
        else                -> "?"
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
