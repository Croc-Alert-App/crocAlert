package crocalert.app.shared.data.local

class InMemoryCameraSettingsDataSource : CameraSettingsDataSource {
    private val store = mutableMapOf<String, Int>()

    override suspend fun getExpectedPerDay(cameraId: String): Int? = store[cameraId]

    override suspend fun setExpectedPerDay(cameraId: String, value: Int) {
        store[cameraId] = value
    }
}
