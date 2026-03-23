package crocalert.app.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.Camera
import crocalert.app.shared.AppModule
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class CamerasViewModel(
    private val cameraRepository: CameraRepository = AppModule.provideCameraRepository(),
    initialCameras: List<CameraUiItem> = emptyList(),
) : ViewModel() {

    // Raw data cache so we can rebuild cards without a network call
    private var rawCameras: List<Camera> = emptyList()
    private val rawDailyStats: MutableMap<String, CameraDailyStatsDto> = mutableMapOf()

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

    private val _sortDescending = MutableStateFlow(false)
    val sortDescending: StateFlow<Boolean> = _sortDescending.asStateFlow()

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
        _cameras, _searchQuery, _selectedFilter, _visibilityFilter, _sortDescending
    ) { cameras, query, filter, visibility, descending ->
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
            .let { list ->
                if (descending) list.sortedByDescending { it.name }
                else list.sortedBy { it.name }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchChange(query: String) { _searchQuery.value = query }
    fun onFilterSelect(filter: CameraFilter) { _selectedFilter.value = filter }
    fun onVisibilityFilterSelect(filter: VisibilityFilter) { _visibilityFilter.value = filter }
    fun toggleSort() { _sortDescending.value = !_sortDescending.value }

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
            val stats = rawDailyStats[camera.id]
            if (stats != null) {
                camera.toUiItem(stats)
            } else {
                // Fallback when daily-stats not yet loaded
                camera.toUiItem(
                    captures = emptyList(),
                    expectedPerDay = camera.expectedImages?.takeIf { it > 0 } ?: DEFAULT_EXPECTED_PER_DAY,
                )
            }
        } + DEMO_CAMERAS
    }

    companion object {
        // TODO: remove after demo — hardcoded cameras to showcase sort + status variety
        private val DEMO_CAMERAS = listOf(
            CameraUiItem(
                id             = "demo-bien",
                name           = "CAM-DEMO-A",
                isActive       = true,
                status         = CameraStatus.Ok,
                lastCapture    = "Hace 15 min",
                imagesSent     = 22,
                imagesExpected = 24,
                captureCount   = 22,
                captureExpected= 24,
                missingCaptures= 2,
                integrityFlags = 0,
            ),
            CameraUiItem(
                id             = "demo-precaucion",
                name           = "CAM-DEMO-B",
                isActive       = true,
                status         = CameraStatus.Attention,
                lastCapture    = "Hace 2 h",
                imagesSent     = 16,
                imagesExpected = 24,
                captureCount   = 16,
                captureExpected= 24,
                missingCaptures= 8,
                integrityFlags = 0,
            ),
        )
    }

    private suspend fun reloadExpectedAndRebuild() {
        rebuildCameras()
    }

    private suspend fun loadData() {
        _isLoading.value = true
        _error.value = null
        try {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            cameraRepository.observeCameras().collect { cameras ->
                rawCameras = cameras
                // Single request for all cameras' daily stats instead of N per-camera calls
                when (val statsResult = cameraRepository.getDailyStatsForAll(today)) {
                    is ApiResult.Success -> {
                        rawDailyStats.clear()
                        statsResult.data.forEach { rawDailyStats[it.cameraId] = it }
                    }
                    is ApiResult.Error -> {
                        _error.value = "Error al obtener estadísticas diarias"
                    }
                }
                rebuildCameras()
                _isLoading.value = false
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Error al cargar las cámaras"
            _isLoading.value = false
        }
    }
}
