package crocalert.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import crocalert.app.shared.AppModule
import crocalert.app.shared.sync.SyncPreferences
import crocalert.app.shared.sync.SyncPreferencesProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val syncPrefsProvider: SyncPreferencesProvider =
        AppModule.provideSyncPreferencesProvider(),
) : ViewModel() {

    val syncPreferences: StateFlow<SyncPreferences> = syncPrefsProvider.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, SyncPreferences())

    fun setAlertsTtl(minutes: Int) {
        viewModelScope.launch { syncPrefsProvider.setAlertsTtl(minutes.coerceIn(1, 1440)) }
    }

    fun setCamerasTtl(minutes: Int) {
        viewModelScope.launch { syncPrefsProvider.setCamerasTtl(minutes.coerceIn(1, 1440)) }
    }
}
