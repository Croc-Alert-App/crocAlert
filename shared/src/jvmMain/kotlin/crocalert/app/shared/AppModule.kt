package crocalert.app.shared

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.shared.data.local.CameraSettingsDataSource
import crocalert.app.shared.data.local.InMemoryCameraSettingsDataSource
import crocalert.app.shared.sync.InMemorySyncPreferencesProvider
import crocalert.app.shared.sync.SyncPreferencesProvider

object AppModule {

    // Desktop JVM target — points to a locally running server instance.
    private const val BASE_URL = "http://localhost:8080"

    fun provideAlertRepository(): AlertRepository =
        createAlertRepository(baseUrl = BASE_URL)

    fun provideCameraRepository(): CameraRepository =
        createCameraRepository(baseUrl = BASE_URL)

    fun provideCameraSettings(): CameraSettingsDataSource =
        InMemoryCameraSettingsDataSource()

    fun provideSyncPreferencesProvider(): SyncPreferencesProvider =
        InMemorySyncPreferencesProvider()
}
