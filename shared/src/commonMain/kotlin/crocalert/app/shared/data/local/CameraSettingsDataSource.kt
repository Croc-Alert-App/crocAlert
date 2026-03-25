package crocalert.app.shared.data.local

interface CameraSettingsDataSource {
    /** Returns the user-persisted value, or null if the user has never set one for this camera. */
    suspend fun getExpectedPerDay(cameraId: String): Int?
    suspend fun setExpectedPerDay(cameraId: String, value: Int)
}
