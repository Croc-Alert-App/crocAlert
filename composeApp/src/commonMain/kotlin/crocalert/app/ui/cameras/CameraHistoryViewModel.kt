package crocalert.app.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.Camera
import crocalert.app.shared.AppModule
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

private const val MIN_EXPECTED = 1
private const val MAX_EXPECTED = 48

// Filename format: CAMXX_YYYY_MM_DD_HH_MM_SS.jpg — last 6 underscore-segments encode the timestamp.
private fun CaptureDto.resolvedCaptureTimeMs(tz: TimeZone): Long? {
    captureTime?.let { return it }
    return runCatching {
        val parts = name.substringBeforeLast(".").split("_")
        if (parts.size < 7) return null
        val n = parts.size
        val dt = LocalDateTime(
            year        = parts[n - 6].toInt(),
            monthNumber = parts[n - 5].toInt(),
            dayOfMonth  = parts[n - 4].toInt(),
            hour        = parts[n - 3].toInt(),
            minute      = parts[n - 2].toInt(),
            second      = parts[n - 1].toInt(),
        )
        dt.toInstant(tz).toEpochMilliseconds()
    }.getOrNull()
}

class CameraHistoryViewModel(
    private val cameraId: String,
    private val cameraName: String,
    private val repository: CameraRepository = AppModule.provideCameraRepository(),
) : ViewModel() {

    private val tz get() = TimeZone.currentSystemDefault()
    private val today get() = Clock.System.todayIn(tz)
    private var currentCamera: Camera? = null
    private val loadMutex = Mutex()

    private val _uiState = MutableStateFlow(
        CameraHistoryUiState(
            cameraId = cameraId,
            cameraName = cameraName,
            selectedDate = today, // open on today so counts match the camera list card
            received = 0,
            missing = 0,
            expected = DEFAULT_EXPECTED_PER_DAY,
            expectedPerDay = DEFAULT_EXPECTED_PER_DAY,
            integrityFlags = 0,
            captureSlots = emptyList(),
            canGoNext = false, // today is always the latest navigable date
            isLoading = true,
        )
    )
    val uiState: StateFlow<CameraHistoryUiState> = _uiState.asStateFlow()

    init {
        // single source of truth for expectedPerDay
        viewModelScope.launch {
            val cameraModel = repository.observeCamera(cameraId).first()
            currentCamera = cameraModel
            val initial = (cameraModel?.expectedImages?.takeIf { it > 0 } ?: DEFAULT_EXPECTED_PER_DAY)
                .coerceIn(MIN_EXPECTED, MAX_EXPECTED)
            _uiState.value = _uiState.value.copy(expectedPerDay = initial, expected = initial)
            loadCaptures()
        }

        // keep name + expectedPerDay in sync with Firebase
        viewModelScope.launch {
            repository.observeCamera(cameraId).collect { camera ->
                currentCamera = camera
                camera?.let { c ->
                    val newExpected = (c.expectedImages?.takeIf { it > 0 } ?: DEFAULT_EXPECTED_PER_DAY)
                        .coerceIn(MIN_EXPECTED, MAX_EXPECTED)
                    val current = _uiState.value
                    _uiState.value = current.copy(cameraName = c.name)
                    if (newExpected != current.expectedPerDay) {
                        _uiState.value = _uiState.value.copy(
                            expectedPerDay = newExpected,
                            isLoading = true,
                        )
                        loadCaptures()
                    }
                }
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
            // Persist back to the camera model (Firebase) — single source of truth shared with edit form
            currentCamera?.let { repository.updateCamera(it.copy(expectedImages = clamped)) }
            loadCaptures()
        }
    }

    private suspend fun loadCaptures() = loadMutex.withLock {
        val state = _uiState.value
        val date = state.selectedDate
        val dateString = date.toString()
        val expectedPerDay = state.expectedPerDay
        val currentToday = today
        val startMs = date.atStartOfDayIn(tz).toEpochMilliseconds()
        val endMs   = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds()
        val dayMs   = endMs - startMs
        val nowMs   = Clock.System.now().toEpochMilliseconds()
        val isToday = date == currentToday

        val currentSlot = if (isToday)
            (((nowMs - startMs).coerceAtLeast(0) * expectedPerDay) / dayMs).toInt()
                .coerceIn(0, expectedPerDay - 1)
        else expectedPerDay

        // server stats are authoritative for header counts; slot grid comes from per-capture data
        val statsResult  = repository.getDailyStats(cameraId, dateString)
        val captureResult = repository.getCapturesByCamera(cameraId)

        if (captureResult is ApiResult.Error && statsResult is ApiResult.Error) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error al cargar historial",
            )
            return@withLock
        }

        // Build the slot grid from individual captures (capture-level detail)
        val captures = (captureResult as? ApiResult.Success)?.data ?: emptyList()
        // Date-range filter uses captureTime only — no filename fallback here.
        // images_per_day is also computed from captureTime, so both stay in sync.
        // Filename fallback is kept only for slot-index positioning below.
        val capturesForDate = captures
            .filter { capture ->
                val t = capture.captureTime ?: 0L
                t >= startMs && t < endMs
            }
            .sortedBy { it.captureTime ?: startMs }

        // Assign each capture to its natural slot; if that slot is already occupied,
        // overflow to the next empty slot (forward then backward). This ensures the
        // number of blue/yellow indicators matches the received image count even when
        // multiple captures arrive within the same time window.
        val slotBuckets = Array(expectedPerDay) { mutableListOf<CaptureDto>() }
        for (capture in capturesForDate) {
            val natural = run {
                val t = (capture.resolvedCaptureTimeMs(tz) ?: startMs).coerceAtLeast(startMs)
                (((t - startMs) * expectedPerDay) / dayMs).toInt().coerceIn(0, expectedPerDay - 1)
            }
            val target = (natural until expectedPerDay).firstOrNull { slotBuckets[it].isEmpty() }
                ?: (0 until natural).firstOrNull { slotBuckets[it].isEmpty() }
            target?.let { slotBuckets[it].add(capture) }
        }

        val slotDurationHours = 24.0 / expectedPerDay
        val slots = (0 until expectedPerDay).map { slotIndex ->
            val slotStartHour = (slotIndex * slotDurationHours).toInt()
            val slotCaptures = slotBuckets[slotIndex]
            val slotState = when {
                slotCaptures.any { it.driveUrl.isNotBlank() } -> CaptureSlotState.Received
                slotCaptures.isNotEmpty()                     -> CaptureSlotState.IntegrityFlag
                isToday && slotIndex > currentSlot            -> CaptureSlotState.Expected
                else                                          -> CaptureSlotState.Missing
            }
            CaptureSlot(hour = slotStartHour, state = slotState)
        }

        // Prefer server stats for the header counts (authoritative); fall back to slot-derived counts
        val serverStats = (statsResult as? ApiResult.Success)?.data
        val received      = serverStats?.receivedImages  ?: slots.count { it.state in setOf(CaptureSlotState.Received, CaptureSlotState.IntegrityFlag) }
        val missing       = serverStats?.missingImages   ?: slots.count { it.state == CaptureSlotState.Missing }
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

}
