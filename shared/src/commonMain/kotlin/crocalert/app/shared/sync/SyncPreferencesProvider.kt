package crocalert.app.shared.sync

import kotlinx.coroutines.flow.Flow

interface SyncPreferencesProvider {
    val preferences: Flow<SyncPreferences>
    suspend fun setAlertsTtl(minutes: Int)
    suspend fun setCamerasTtl(minutes: Int)
    suspend fun setAlertWindowDays(days: Int)
}
