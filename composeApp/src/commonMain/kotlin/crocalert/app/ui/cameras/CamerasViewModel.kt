package crocalert.app.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CamerasViewModel(
    initialCameras: List<CameraUiItem> = CamerasMockData.cameras
) : ViewModel() {

    private val _cameras = MutableStateFlow(initialCameras)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(CameraFilter.All)
    val selectedFilter: StateFlow<CameraFilter> = _selectedFilter.asStateFlow()

    /** Per-status counts from the full unfiltered list, used to label the filter chips. */
    val statusCounts: StateFlow<Map<CameraStatus, Int>> = _cameras
        .map { list -> CameraStatus.entries.associateWith { s -> list.count { it.status == s } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Camera list after applying search query and status filter, sorted by severity. */
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CamerasMockData.cameras)

    fun onSearchChange(query: String) { _searchQuery.value = query }

    fun onFilterSelect(filter: CameraFilter) { _selectedFilter.value = filter }

    fun clearSearch() {
        _searchQuery.value = ""
        _selectedFilter.value = CameraFilter.All
    }
}
