package crocalert.app.ui.cameras

import kotlinx.datetime.LocalDate

data class CameraHistoryUiState(
    val cameraId: String,
    val cameraName: String,
    val selectedDate: LocalDate,
    val received: Int,
    val missing: Int,
    val expected: Int,
    val integrityFlags: Int,
    val captureSlots: List<CaptureSlot>,
    val canGoNext: Boolean,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class CaptureSlot(val hour: Int, val state: CaptureSlotState)

enum class CaptureSlotState { Received, Missing, Expected, IntegrityFlag }
