package crocalert.app.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.shared.AppModule
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CamerasViewModel(
    private val cameraRepository: CameraRepository = AppModule.provideCameraRepository()
) : ViewModel() {

    private val _cameras = MutableStateFlow<List<CameraUiItem>>(emptyList())

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

    init {
        viewModelScope.launch { loadData() }
    }

    fun toggleExpand(id: String) {
        _expandedCameraId.value = if (_expandedCameraId.value == id) null else id
    }

    val statusCounts: StateFlow<Map<CameraStatus, Int>> = _cameras
        .map { list -> CameraStatus.entries.associateWith { s -> list.count { it.status == s } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val filteredCameras: StateFlow<List<CameraUiItem>> = combine(
        _cameras, _searchQuery, _selectedFilter
    ) { cameras, query, filter ->
        cameras
            .filter { camera ->
                filter.matches(camera.status) &&
                (query.isBlank() ||
                    camera.name.contains(query, ignoreCase = true) ||
                    camera.id.contains(query, ignoreCase = true))
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

    private suspend fun loadData() {
        _isLoading.value = true
        _error.value = null
        try {
            cameraRepository.observeCameras().collect { cameras ->
                _cameras.value = cameras.map { camera ->
                    val captures = when (val res = cameraRepository.getCapturesByCamera(camera.id)) {
                        is ApiResult.Success -> res.data
                        is ApiResult.Error   -> emptyList()
                    }
                    camera.toUiItem(captures)
                }
                _isLoading.value = false
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to load cameras"
            _isLoading.value = false
        }
    }
}
