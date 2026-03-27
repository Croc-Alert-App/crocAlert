package crocalert.app.shared.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cameraSettingsStore by preferencesDataStore(name = "camera_settings")

class DataStoreCameraSettingsDataSource(private val context: Context) : CameraSettingsDataSource {

    override suspend fun getExpectedPerDay(cameraId: String): Int? =
        context.cameraSettingsStore.data
            .map { it[intPreferencesKey("expected_$cameraId")] }
            .first()

    override suspend fun setExpectedPerDay(cameraId: String, value: Int) {
        context.cameraSettingsStore.edit { prefs ->
            prefs[intPreferencesKey("expected_$cameraId")] = value
        }
    }
}
