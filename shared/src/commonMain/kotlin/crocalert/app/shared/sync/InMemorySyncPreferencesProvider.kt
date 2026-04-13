package crocalert.app.shared.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemorySyncPreferencesProvider : SyncPreferencesProvider {

    private val _preferences = MutableStateFlow(SyncPreferences())
    override val preferences: Flow<SyncPreferences> = _preferences.asStateFlow()

    override suspend fun setAlertsTtl(minutes: Int) {
        _preferences.update { it.copy(alertsTtlMinutes = minutes) }
    }

    override suspend fun setCamerasTtl(minutes: Int) {
        _preferences.update { it.copy(camerasTtlMinutes = minutes) }
    }

    override suspend fun setAlertWindowDays(days: Int) {
        _preferences.update { it.copy(alertWindowDays = days) }
    }
}
