package crocalert.app.feature.alerts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.domain.repository.SiteRepository
import crocalert.app.model.Alert
import crocalert.app.model.Camera
import crocalert.app.model.Site
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface AlertDetailUiState {
    data object Loading : AlertDetailUiState
    data class Success(
        val alert: Alert,
        val camera: Camera?,
        val site: Site?,
        val capture: CaptureDto?,
    ) : AlertDetailUiState
    data class Error(val message: String) : AlertDetailUiState
}

class AlertDetailViewModel(
    private val alertId: String,
    private val alertRepository: AlertRepository,
    private val cameraRepository: CameraRepository,
    private val siteRepository: SiteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlertDetailUiState>(AlertDetailUiState.Loading)
    val uiState: StateFlow<AlertDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _uiState.value = AlertDetailUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                // observeAlert() emits the local cache immediately (may be null on cold start)
                // then re-emits once syncIfStale() populates it. filterNotNull().first()
                // waits for the first non-null emission rather than racing the background sync.
                val alert = withTimeoutOrNull(10_000L) {
                    alertRepository.observeAlert(alertId).filterNotNull().first()
                } ?: throw IllegalArgumentException("Alerta $alertId no encontrada")

                val camera: Camera? = if (alert.cameraId.isNotBlank()) {
                    cameraRepository.observeCamera(alert.cameraId).first()
                } else null

                val siteId = camera?.siteId
                val site: Site? = if (siteId != null) {
                    siteRepository.observeSite(siteId).first()
                } else null

                val capture: CaptureDto? = if (alert.cameraId.isNotBlank() && alert.captureId.isNotBlank()) {
                    when (val result = cameraRepository.getCapturesByCamera(alert.cameraId)) {
                        is ApiResult.Success -> result.data.firstOrNull { it.id == alert.captureId }
                        is ApiResult.Error -> null
                    }
                } else null

                _uiState.value = AlertDetailUiState.Success(alert, camera, site, capture)
            } catch (e: Exception) {
                _uiState.value = AlertDetailUiState.Error(e.message ?: "Error al cargar la alerta")
            }
        }
    }
}
