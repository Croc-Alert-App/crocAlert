package crocalert.app.shared

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import crocalert.app.db.CrocAlertDb
import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.shared.data.local.CameraSettingsDataSource
import crocalert.app.shared.data.local.DataStoreCameraSettingsDataSource
import crocalert.app.shared.data.local.SqlDelightAlertLocalDataSource
import crocalert.app.shared.data.local.SqlDelightCameraLocalDataSource
import crocalert.app.shared.data.local.SqlDelightSiteLocalDataSource
import crocalert.app.shared.data.remote.AlertRemoteDataSourceImpl
import crocalert.app.shared.data.remote.CameraRemoteDataSourceImpl
import crocalert.app.shared.data.remote.SiteRemoteDataSourceImpl
import crocalert.app.shared.data.repository.AlertRepositoryImpl
import crocalert.app.shared.data.repository.CameraRepositoryImpl
import crocalert.app.shared.data.repository.SiteRepositoryImpl
import crocalert.app.shared.network.ApiRoutes
import crocalert.app.shared.network.HttpClientFactory
import crocalert.app.shared.sync.DataStoreSyncPreferencesProvider
import crocalert.app.shared.sync.WorkManagerSyncScheduler

object AppModule {

    // Android emulator loopback alias — override with a real server URL before release.
    private const val BASE_URL = "http://10.0.2.2:8080"

    private lateinit var alertRepo: AlertRepositoryImpl
    private lateinit var cameraRepo: CameraRepositoryImpl
    private lateinit var siteRepo: SiteRepositoryImpl
    private lateinit var cameraSettings: CameraSettingsDataSource
    private lateinit var syncPrefsProvider: DataStoreSyncPreferencesProvider

    /** Call once from MainActivity before launching the Compose tree. */
    fun setup(context: Context) {
        ApiRoutes.BASE = BASE_URL

        val httpClient = HttpClientFactory.create()

        val driver = AndroidSqliteDriver(CrocAlertDb.Schema, context, "crocalert.db")
        val db = CrocAlertDb(driver)

        val alertLocal  = SqlDelightAlertLocalDataSource(db.alertQueries)
        val cameraLocal = SqlDelightCameraLocalDataSource(db.cameraQueries)
        val siteLocal   = SqlDelightSiteLocalDataSource(db.siteQueries)

        syncPrefsProvider = DataStoreSyncPreferencesProvider(context)
        val syncPrefsFlow = syncPrefsProvider.preferences
        cameraSettings = DataStoreCameraSettingsDataSource(context)

        alertRepo = AlertRepositoryImpl(
            remote    = AlertRemoteDataSourceImpl(httpClient, BASE_URL),
            local     = alertLocal,
            syncPrefs = syncPrefsFlow,
        )
        cameraRepo = CameraRepositoryImpl(
            remote    = CameraRemoteDataSourceImpl(httpClient, BASE_URL),
            local     = cameraLocal,
            syncPrefs = syncPrefsFlow,
        )
        siteRepo = SiteRepositoryImpl(
            remote    = SiteRemoteDataSourceImpl(httpClient, BASE_URL),
            local     = siteLocal,
            syncPrefs = syncPrefsFlow,
        )

        val scheduler = WorkManagerSyncScheduler(context)
        scheduler.scheduleAlertSync(intervalMinutes = 5)
        scheduler.scheduleCameraSync(intervalMinutes = 15)
        scheduler.scheduleSiteSync(intervalMinutes = 60)
    }

    fun provideAlertRepository(): AlertRepository  = alertRepo
    fun provideCameraRepository(): CameraRepository = cameraRepo
    fun provideCameraSettings(): CameraSettingsDataSource = cameraSettings
    fun provideSyncPreferencesProvider(): DataStoreSyncPreferencesProvider = syncPrefsProvider

    // Called by sync workers to force a full refresh regardless of TTL.
    suspend fun syncAlerts()  = alertRepo.forceSync()
    suspend fun syncCameras() = cameraRepo.forceSync()
    suspend fun syncSites()   = siteRepo.forceSync()
}
