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
import kotlinx.coroutines.Job
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
    private var statsJob: Job? = null

    private val _cameras = MutableStateFlow<List<CameraUiItem>>(initialCameras)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CameraFilter.All)
    val selectedFilter: StateFlow<CameraFilter> = _selectedFilter.asStateFlow()

    private val _expandedCameraId = MutableStateFlow<String?>(null)
    val expandedCameraId: StateFlow<String?> = _expandedCameraId.asStateFlow()

    private val _historyCamera = MutableStateFlow<CameraUiItem?>(null)
    val historyCamera: StateFlow<CameraUiItem?> = _historyCamera.asStateFlow()

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

    val statusCounts: StateFlow<Map<CameraStatus, Int>> = _cameras
        .map { list -> CameraStatus.entries.associateWith { s -> list.count { it.status == s } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val filteredCameras: StateFlow<List<CameraUiItem>> = combine(
        _cameras, _searchQuery, _selectedFilter
    ) { cameras, query, filter ->
        cameras
            .filter { camera -> filter.matches(camera.status) }
            .filter { camera ->
                query.isBlank() ||
                    camera.name.contains(query, ignoreCase = true) ||
                    camera.id.contains(query, ignoreCase = true)
            }
            .sortedBy { it.status.severity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchChange(query: String) { _searchQuery.value = query }
    fun onFilterSelect(filter: CameraFilter) { _selectedFilter.value = filter }

    fun clearSearch() {
        _searchQuery.value = ""
        _selectedFilter.value = CameraFilter.All
    }

    fun retry() { viewModelScope.launch { loadData() } }

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
        }
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
                // Render immediately from local cache — no network wait
                rebuildCameras()
                _isLoading.value = false
                // Fetch stats in the background; cancel any in-flight request first
                statsJob?.cancel()
                statsJob = viewModelScope.launch {
                    when (val statsResult = cameraRepository.getDailyStatsForAll(today)) {
                        is ApiResult.Success -> {
                            rawDailyStats.clear()
                            statsResult.data.forEach { rawDailyStats[it.cameraId] = it }
                            rebuildCameras()
                        }
                        is ApiResult.Error -> {
                            _error.value = "Error al obtener estadísticas diarias"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Error al cargar las cámaras"
            _isLoading.value = false
        }
    }
}
