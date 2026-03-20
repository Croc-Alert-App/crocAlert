package crocalert.app.shared

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.shared.network.ApiRoutes

object AppModule {

    // Android emulator loopback alias — override with a real server URL before release.
    private const val BASE_URL = "http://10.0.2.2:8080"

    /** Call once from MainActivity before launching the Compose tree. */
    fun setup() {
        ApiRoutes.BASE = BASE_URL
    }

    fun provideAlertRepository(): AlertRepository =
        createAlertRepository(baseUrl = BASE_URL)

    fun provideCameraRepository(): CameraRepository =
        createCameraRepository(baseUrl = BASE_URL)
}
