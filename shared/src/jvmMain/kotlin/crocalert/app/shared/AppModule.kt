package crocalert.app.shared

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository

object AppModule {

    // Desktop JVM target — points to a locally running server instance.
    private const val BASE_URL = "http://localhost:8080"

    fun provideAlertRepository(): AlertRepository =
        createAlertRepository(baseUrl = BASE_URL)

    fun provideCameraRepository(): CameraRepository =
        createCameraRepository(baseUrl = BASE_URL)
}
