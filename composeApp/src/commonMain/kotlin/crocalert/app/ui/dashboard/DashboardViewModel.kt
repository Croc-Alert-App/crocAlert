package crocalert.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.ui.components.DashboardTab
import crocalert.app.ui.components.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.Syncing)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSynced = MutableStateFlow("")
    val lastSynced: StateFlow<String> = _lastSynced.asStateFlow()

    private val _selectedTab = MutableStateFlow(DashboardTab.Home)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun retry() {
        _uiState.value = DashboardUiState.Loading
        _syncStatus.value = SyncStatus.Syncing
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = try {
                val data = DashboardUiState.Success(DashboardMockData.load())
                _syncStatus.value = SyncStatus.Synced
                _lastSynced.value = "ahora mismo"
                data
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error
                DashboardUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
