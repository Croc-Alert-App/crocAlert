package crocalert.app.ui.auth

/**
 * Manages the "remember device" session flag.
 *
 * Current implementation: **in-memory only** — state is lost when the process is killed.
 *
 * TODO: Replace backing store with DataStore<Preferences> for true persistence.
 *   Recommended library: androidx.datastore:datastore-preferences (KMP-compatible since 1.1.x)
 *
 *   Steps to upgrade:
 *   1. Add `implementation(libs.datastore.preferences)` to composeApp/build.gradle.kts
 *   2. Create a platform-specific DataStore instance via expect/actual (androidMain / jvmMain / iosMain)
 *   3. Replace the `_isDeviceRemembered` Boolean below with a DataStore read/write
 *   4. Expose `isDeviceRememberedFlow: Flow<Boolean>` and collect it in SplashScreen
 */
object SessionManager {

    private var _isDeviceRemembered: Boolean = false

    /** Returns true if the user previously chose "Recordar dispositivo". */
    val isDeviceRemembered: Boolean
        get() = _isDeviceRemembered

    /** Call on successful login when the user checked "Recordar dispositivo". */
    fun rememberDevice() {
        _isDeviceRemembered = true
    }

    /** Call on logout — clears the remembered session. */
    fun forgetDevice() {
        _isDeviceRemembered = false
    }
}
