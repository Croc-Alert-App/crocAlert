package crocalert.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class DashboardViewModel(
    private val loadDashboard: suspend () -> DashboardData = { DashboardMockData.load() }
) : ViewModel() {

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

    // H1 fix: retry properly re-triggers loadData() instead of just changing a visual flag
    fun retry() {
        _uiState.value = DashboardUiState.Loading
        _syncStatus.value = SyncStatus.Syncing
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = try {
                val data = DashboardUiState.Success(loadDashboard())
                _syncStatus.value = SyncStatus.Synced
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                _lastSynced.value = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
                data
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error
                DashboardUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
