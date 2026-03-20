package crocalert.app.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.shared.AppModule
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

private const val EXPECTED_PER_DAY = 24

class CameraHistoryViewModel(
    private val cameraId: String,
    private val cameraName: String,
    private val repository: CameraRepository = AppModule.provideCameraRepository(),
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
            expected = EXPECTED_PER_DAY,
            integrityFlags = 0,
            captureSlots = emptyList(),
            canGoNext = false,
            isLoading = true,
        )
    )
    val uiState: StateFlow<CameraHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadCaptures() }
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

    private suspend fun loadCaptures() {
        val date = _uiState.value.selectedDate
        val startMs = date.atStartOfDayIn(tz).toEpochMilliseconds()
        val endMs = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
        val nowInstant = Clock.System.now()
        val currentHour = nowInstant.toLocalDateTime(tz).hour
        val isToday = date == today

        when (val result = repository.getCapturesByCamera(cameraId)) {
            is ApiResult.Success -> {
                val capturesForDate = result.data.filter { capture ->
                    val t = capture.captureTime ?: 0L
                    t >= startMs && t < endMs
                }

                val byHour = capturesForDate.groupBy { capture ->
                    Instant.fromEpochMilliseconds(capture.captureTime ?: 0L)
                        .toLocalDateTime(tz).hour
                }

                val slots = (0 until EXPECTED_PER_DAY).map { hour ->
                    val captures = byHour[hour]
                    val slotState = when {
                        captures != null && captures.any { it.driveUrl.isNotBlank() } ->
                            CaptureSlotState.Received
                        captures != null ->
                            CaptureSlotState.IntegrityFlag
                        isToday && hour > currentHour ->
                            CaptureSlotState.Expected
                        else ->
                            CaptureSlotState.Missing
                    }
                    CaptureSlot(hour = hour, state = slotState)
                }

                // Recibidas = captures that arrived (including those with integrity issues)
                val received = slots.count { it.state in setOf(CaptureSlotState.Received, CaptureSlotState.IntegrityFlag) }
                val missing = slots.count { it.state == CaptureSlotState.Missing }
                val integrityFlags = slots.count { it.state == CaptureSlotState.IntegrityFlag }

                _uiState.value = _uiState.value.copy(
                    received = received,
                    missing = missing,
                    expected = EXPECTED_PER_DAY,
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
