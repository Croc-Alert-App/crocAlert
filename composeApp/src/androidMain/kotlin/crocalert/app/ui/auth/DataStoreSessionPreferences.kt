package crocalert.app.ui.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionStore by preferencesDataStore(name = "session_prefs")

class DataStoreSessionPreferences(private val context: Context) : SessionPreferences {

    private val savedEmailKey      = stringPreferencesKey("saved_email")
    private val sessionExpiresAtKey = longPreferencesKey("session_expires_at")

    override suspend fun getSavedEmail(): String? =
        context.sessionStore.data.map { it[savedEmailKey] }.first()

    override suspend fun setSavedEmail(email: String?) {
        context.sessionStore.edit { prefs ->
            if (email != null) prefs[savedEmailKey] = email
            else prefs.remove(savedEmailKey)
        }
    }

    override suspend fun getSessionExpiresAt(): Long? =
        context.sessionStore.data.map { it[sessionExpiresAtKey] }.first()

    override suspend fun setSessionExpiresAt(expiresAt: Long?) {
        context.sessionStore.edit { prefs ->
            if (expiresAt != null) prefs[sessionExpiresAtKey] = expiresAt
            else prefs.remove(sessionExpiresAtKey)
        }
    }
}
