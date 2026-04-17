package crocalert.app.ui.auth

import crocalert.app.ui.auth.SessionPreferences
import java.util.prefs.Preferences

class DesktopSessionPreferences : SessionPreferences {
    private val prefs = Preferences.userRoot().node("crocalert")

    override suspend fun getSavedEmail(): String? =
        prefs.get("saved_email", null)

    override suspend fun setSavedEmail(email: String?) {
        if (email == null) prefs.remove("saved_email")
        else prefs.put("saved_email", email)
    }

    override suspend fun getSessionExpiresAt(): Long? =
        if (prefs.get("session_expires_at", null) != null) {
            prefs.getLong("session_expires_at", 0L)
        } else {
            null
        }

    override suspend fun setSessionExpiresAt(expiresAt: Long?) {
        if (expiresAt == null) prefs.remove("session_expires_at")
        else prefs.putLong("session_expires_at", expiresAt)
    }

    override suspend fun updateSession(email: String?, expiresAt: Long?) {
        if (email == null) prefs.remove("saved_email")
        else prefs.put("saved_email", email)

        if (expiresAt == null) prefs.remove("session_expires_at")
        else prefs.putLong("session_expires_at", expiresAt)
    }
}