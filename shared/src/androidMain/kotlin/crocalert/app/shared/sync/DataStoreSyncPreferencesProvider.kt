package crocalert.app.shared.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sync_preferences")

class DataStoreSyncPreferencesProvider(private val context: Context) : SyncPreferencesProvider {

    private val alertsTtlKey       = intPreferencesKey("alerts_ttl_minutes")
    private val camerasTtlKey      = intPreferencesKey("cameras_ttl_minutes")
    private val sitesTtlKey        = intPreferencesKey("sites_ttl_minutes")
    private val alertWindowDaysKey = intPreferencesKey("alert_window_days")

    override val preferences: Flow<SyncPreferences> = context.dataStore.data.map { prefs ->
        SyncPreferences(
            alertsTtlMinutes  = prefs[alertsTtlKey]       ?: SyncPreferences().alertsTtlMinutes,
            camerasTtlMinutes = prefs[camerasTtlKey]      ?: SyncPreferences().camerasTtlMinutes,
            sitesTtlMinutes   = prefs[sitesTtlKey]        ?: SyncPreferences().sitesTtlMinutes,
            alertWindowDays   = prefs[alertWindowDaysKey] ?: SyncPreferences().alertWindowDays,
        )
    }

    override suspend fun setAlertsTtl(minutes: Int) {
        context.dataStore.edit { it[alertsTtlKey] = minutes }
    }

    override suspend fun setCamerasTtl(minutes: Int) {
        context.dataStore.edit { it[camerasTtlKey] = minutes }
    }

    override suspend fun setAlertWindowDays(days: Int) {
        context.dataStore.edit { it[alertWindowDaysKey] = days }
    }
}
