package crocalert.app.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.Camera
import crocalert.app.shared.AppModule
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.data.local.CameraSettingsDataSource
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DEFAULT_EXPECTED_PER_DAY = 24

class CamerasViewModel(
    private val cameraRepository: CameraRepository = AppModule.provideCameraRepository(),
    private val cameraSettings: CameraSettingsDataSource = AppModule.provideCameraSettings(),
    initialCameras: List<CameraUiItem> = emptyList()
) : ViewModel() {

    // Raw data cache so we can rebuild cards without a network call
    private var rawCameras: List<Camera> = emptyList()
    private val rawCaptures: MutableMap<String, List<CaptureDto>> = mutableMapOf()
    private val rawExpected: MutableMap<String, Int> = mutableMapOf()

    private val _cameras = MutableStateFlow<List<CameraUiItem>>(initialCameras)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CameraFilter.All)
    val selectedFilter: StateFlow<CameraFilter> = _selectedFilter.asStateFlow()

    private val _visibilityFilter = MutableStateFlow(VisibilityFilter.Active)
    val visibilityFilter: StateFlow<VisibilityFilter> = _visibilityFilter.asStateFlow()

    private val _expandedCameraId = MutableStateFlow<String?>(null)
    val expandedCameraId: StateFlow<String?> = _expandedCameraId.asStateFlow()

    private val _historyCamera = MutableStateFlow<CameraUiItem?>(null)
    val historyCamera: StateFlow<CameraUiItem?> = _historyCamera.asStateFlow()

    // ── Camera form state ──────────────────────────────────────────────────────
    private val _showCameraForm = MutableStateFlow(false)
    val showCameraForm: StateFlow<Boolean> = _showCameraForm.asStateFlow()

    /** null → create mode; non-null → edit mode with pre-filled data */
    private val _cameraToEdit = MutableStateFlow<Camera?>(null)
    val cameraToEdit: StateFlow<Camera?> = _cameraToEdit.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    init {
        if (initialCameras.isEmpty()) {
            viewModelScope.launch { loadData() }
        }
    }

    fun toggleExpand(id: String) {
        _expandedCameraId.value = if (_expandedCameraId.value == id) null else id
    }

    fun openHistory(camera: CameraUiItem) { _historyCamera.value = camera }

    fun closeHistory() {
        _historyCamera.value = null
        viewModelScope.launch { reloadExpectedAndRebuild() }
    }

    fun openEditCamera() {
        val id = _historyCamera.value?.id ?: return
        _cameraToEdit.value = rawCameras.firstOrNull { it.id == id }
        _saveError.value = null
        _showCameraForm.value = true
    }

    fun dismissCameraForm() {
        _showCameraForm.value = false
        _saveError.value = null
    }

    fun saveCamera(
        name: String,
        isActive: Boolean,
        siteId: String?,
        expectedImages: Int?,
        createdAtMs: Long?,
        installedAtMs: Long?,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _saveError.value = "El nombre no puede estar vacío"
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            try {
                val existing = _cameraToEdit.value ?: return@launch
                val siteTrimmed = siteId?.trim()?.takeIf { it.isNotBlank() }
                cameraRepository.updateCamera(
                    existing.copy(
                        name           = trimmedName,
                        isActive       = isActive,
                        siteId         = siteTrimmed,
                        expectedImages = expectedImages,
                        createdAt      = createdAtMs ?: existing.createdAt,
                        installedAt    = installedAtMs ?: existing.installedAt,
                    )
                )
                _showCameraForm.value = false
            } catch (e: Exception) {
                _saveError.value = e.message ?: "Error al guardar la cámara"
            } finally {
                _isSaving.value = false
            }
        }
    }

    val statusCounts: StateFlow<Map<CameraStatus, Int>> = _cameras
        .map { list -> CameraStatus.entries.associateWith { s -> list.count { it.isActive && it.status == s } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val filteredCameras: StateFlow<List<CameraUiItem>> = combine(
        _cameras, _searchQuery, _selectedFilter, _visibilityFilter
    ) { cameras, query, filter, visibility ->
        cameras
            .filter { camera ->
                when (visibility) {
                    VisibilityFilter.Active  -> camera.isActive
                    VisibilityFilter.Deleted -> !camera.isActive
                    VisibilityFilter.All     -> true
                }
            }
            .filter { camera ->
                visibility == VisibilityFilter.Deleted || filter.matches(camera.status)
            }
            .filter { camera ->
                query.isBlank() ||
                    camera.name.contains(query, ignoreCase = true) ||
                    camera.id.contains(query, ignoreCase = true)
            }
            .sortedBy { it.status.severity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchChange(query: String) { _searchQuery.value = query }
    fun onFilterSelect(filter: CameraFilter) { _selectedFilter.value = filter }
    fun onVisibilityFilterSelect(filter: VisibilityFilter) { _visibilityFilter.value = filter }

    fun activateCamera(cameraId: String) {
        viewModelScope.launch {
            try {
                val camera = rawCameras.firstOrNull { it.id == cameraId } ?: return@launch
                cameraRepository.updateCamera(camera.copy(isActive = true))
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al activar la cámara"
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _selectedFilter.value = CameraFilter.All
    }

    fun retry() { viewModelScope.launch { loadData() } }

    fun deleteCamera(cameraId: String) {
        viewModelScope.launch {
            try {
                cameraRepository.deleteCamera(cameraId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al eliminar la cámara"
            }
        }
    }

    private fun rebuildCameras() {
        _cameras.value = rawCameras.map { camera ->
            camera.toUiItem(
                captures = rawCaptures[camera.id] ?: emptyList(),
                // User DataStore override → camera model default → hardcoded default
                expectedPerDay = rawExpected[camera.id]
                    ?: camera.expectedImages
                    ?: DEFAULT_EXPECTED_PER_DAY,
            )
        }
    }

    private suspend fun reloadExpectedAndRebuild() {
        for (camera in rawCameras) {
            val stored = cameraSettings.getExpectedPerDay(camera.id)
            if (stored != null) rawExpected[camera.id] = stored else rawExpected.remove(camera.id)
        }
        rebuildCameras()
    }

    private suspend fun loadData() {
        _isLoading.value = true
        _error.value = null
        try {
            cameraRepository.observeCameras().collect { cameras ->
                rawCameras = cameras
                for (camera in rawCameras) {
                    rawCaptures[camera.id] = when (val res = cameraRepository.getCapturesByCamera(camera.id)) {
                        is ApiResult.Success -> res.data
                        is ApiResult.Error   -> emptyList()
                    }
                    val stored = cameraSettings.getExpectedPerDay(camera.id)
                    if (stored != null) rawExpected[camera.id] = stored else rawExpected.remove(camera.id)
                }
                rebuildCameras()
                _isLoading.value = false
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to load cameras"
            _isLoading.value = false
        }
    }
}
