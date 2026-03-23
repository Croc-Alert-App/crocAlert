package crocalert.app.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.shared.AppModule
import crocalert.app.shared.data.local.CameraSettingsDataSource
import crocalert.app.shared.network.ApiResult
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
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

private const val DEFAULT_EXPECTED_PER_DAY = 24
private const val MIN_EXPECTED = 1
private const val MAX_EXPECTED = 48

class CameraHistoryViewModel(
    private val cameraId: String,
    private val cameraName: String,
    private val repository: CameraRepository = AppModule.provideCameraRepository(),
    private val cameraSettings: CameraSettingsDataSource = AppModule.provideCameraSettings(),
) : ViewModel() {

    private val tz = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(tz)

    private val _uiState = MutableStateFlow(
        CameraHistoryUiState(
            cameraId = cameraId,
            cameraName = cameraName,
            selectedDate = today,
            received = 0,
            missing = 0,
            expected = DEFAULT_EXPECTED_PER_DAY,
            expectedPerDay = DEFAULT_EXPECTED_PER_DAY,
            integrityFlags = 0,
            captureSlots = emptyList(),
            canGoNext = false,
            isLoading = true,
        )
    )
    val uiState: StateFlow<CameraHistoryUiState> = _uiState.asStateFlow()

    init {
        // One-time init: load persisted preference, fall back to camera model, then default
        viewModelScope.launch {
            val stored      = cameraSettings.getExpectedPerDay(cameraId)
            val cameraModel = repository.observeCamera(cameraId).first()
            val initial     = stored ?: cameraModel?.expectedImages ?: DEFAULT_EXPECTED_PER_DAY
            _uiState.value = _uiState.value.copy(expectedPerDay = initial, expected = initial)
            loadCaptures()
        }

        // Continuously observe camera for reactive name updates
        viewModelScope.launch {
            repository.observeCamera(cameraId).collect { camera ->
                camera?.let { _uiState.value = _uiState.value.copy(cameraName = it.name) }
            }
        }
    }

    fun prevDay() {
        val newDate = _uiState.value.selectedDate.minus(1, DateTimeUnit.DAY)
        _uiState.value = _uiState.value.copy(
            selectedDate = newDate,
            canGoNext = newDate < today,
            isLoading = true,
        )
        viewModelScope.launch { loadCaptures() }
    }

    fun nextDay() {
        val current = _uiState.value.selectedDate
        if (current >= today) return
        val newDate = current.plus(1, DateTimeUnit.DAY)
        _uiState.value = _uiState.value.copy(
            selectedDate = newDate,
            canGoNext = newDate < today,
            isLoading = true,
        )
        viewModelScope.launch { loadCaptures() }
    }

    fun setExpectedPerDay(value: Int) {
        val clamped = value.coerceIn(MIN_EXPECTED, MAX_EXPECTED)
        _uiState.value = _uiState.value.copy(expectedPerDay = clamped, isLoading = true)
        viewModelScope.launch {
            cameraSettings.setExpectedPerDay(cameraId, clamped)
            loadCaptures()
        }
    }

    private suspend fun loadCaptures() {
        val state = _uiState.value
        val date = state.selectedDate
        val expectedPerDay = state.expectedPerDay
        val dayMs = 24 * 60 * 60 * 1000L
        val startMs = date.atStartOfDayIn(tz).toEpochMilliseconds()
        val endMs = startMs + dayMs
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val isToday = date == today

        // Current slot index within today (0-based)
        val currentSlot = if (isToday)
            (((nowMs - startMs).coerceAtLeast(0) * expectedPerDay) / dayMs).toInt()
                .coerceIn(0, expectedPerDay - 1)
        else expectedPerDay

        when (val result = repository.getCapturesByCamera(cameraId)) {
            is ApiResult.Success -> {
                val capturesForDate = result.data.filter { capture ->
                    val t = capture.captureTime ?: 0L
                    t >= startMs && t < endMs
                }

                // Assign each capture to a slot index based on its position in the day
                val bySlot = capturesForDate.groupBy { capture ->
                    val t = (capture.captureTime ?: startMs).coerceAtLeast(startMs)
                    (((t - startMs) * expectedPerDay) / dayMs).toInt()
                        .coerceIn(0, expectedPerDay - 1)
                }

                val slotDurationHours = 24.0 / expectedPerDay
                val slots = (0 until expectedPerDay).map { slotIndex ->
                    val slotStartHour = (slotIndex * slotDurationHours).toInt()
                    val captures = bySlot[slotIndex]
                    val slotState = when {
                        captures != null && captures.any { it.driveUrl.isNotBlank() } ->
                            CaptureSlotState.Received
                        captures != null ->
                            CaptureSlotState.IntegrityFlag
                        isToday && slotIndex > currentSlot ->
                            CaptureSlotState.Expected
                        else ->
                            CaptureSlotState.Missing
                    }
                    CaptureSlot(hour = slotStartHour, state = slotState)
                }

                val received = slots.count { it.state in setOf(CaptureSlotState.Received, CaptureSlotState.IntegrityFlag) }
                val missing = slots.count { it.state == CaptureSlotState.Missing }
                val integrityFlags = slots.count { it.state == CaptureSlotState.IntegrityFlag }

                _uiState.value = _uiState.value.copy(
                    received = received,
                    missing = missing,
                    expected = expectedPerDay,
                    integrityFlags = integrityFlags,
                    captureSlots = slots,
                    isLoading = false,
                    error = null,
                )
            }
            is ApiResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar historial",
                )
            }
        }
    }

}
