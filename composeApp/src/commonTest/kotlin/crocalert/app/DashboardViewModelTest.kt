package crocalert.app

import crocalert.app.domain.repository.CameraRepository
import crocalert.app.feature.alerts.data.MockAlertRepository
import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto
import crocalert.app.shared.network.ApiResult
import crocalert.app.shared.sync.InMemorySyncPreferencesProvider
import crocalert.app.ui.dashboard.DashboardTab
import crocalert.app.ui.dashboard.DashboardUiState
import crocalert.app.ui.dashboard.DashboardViewModel
import crocalert.app.ui.dashboard.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

/**
 * Unit tests for [DashboardViewModel].
 *
 * The ViewModel makes real repository calls, so tests inject fake repositories
 * that return controlled ApiResult values.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val successDashboard = CameraMonitoringDashboardDto(
        date = "2026-04-12",
        totalCameras = 10,
        activeCameras = 8,
        expectedImagesTotal = 100,
        receivedImagesTotal = 95,
        missingImagesTotal = 5,
        extraImagesTotal = 0,
        globalCaptureRate = 0.95,
        healthyCameras = 8,
        cautionCameras = 1,
        riskCameras = 1,
        healthyRate = 0.80,
        operationalRate = 0.90,
        cameras = emptyList()
    )

    private fun fakeCameraRepo(
        dashboardResult: ApiResult<CameraMonitoringDashboardDto> = ApiResult.Success(successDashboard),
    ) = object : CameraRepository {
        override fun observeCameras(siteId: String?): Flow<List<Camera>> = flowOf(emptyList())
        override fun observeCamera(cameraId: String): Flow<Camera?> = flowOf(null)
        override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> = ApiResult.Success(emptyList())
        override suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto> = ApiResult.Error("not implemented", 501)
        override suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>> = ApiResult.Success(emptyList())
        override suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto> = dashboardResult
        override suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto> = ApiResult.Error("not used", 501)
        override suspend fun createCamera(camera: Camera): String = ""
        override suspend fun updateCamera(camera: Camera) {}
        override suspend fun deleteCamera(cameraId: String) {}
        override suspend fun refresh() {}
    }

    private fun vm(
        cameraRepo: CameraRepository = fakeCameraRepo(),
        alertRepo: MockAlertRepository = MockAlertRepository(),
    ) = DashboardViewModel(
        alertRepository = alertRepo,
        cameraRepository = cameraRepo,
        syncPrefsProvider = InMemorySyncPreferencesProvider(),
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Success path ──────────────────────────────────────────────────────────

    @Test
    fun `uiState becomes Success after repositories return data`() = runTest {
        val v = vm()
        advanceUntilIdle()
        assertIs<DashboardUiState.Success>(v.uiState.value)
    }

    @Test
    fun `Success state contains activeCameras from dashboard dto`() = runTest {
        val v = vm()
        advanceUntilIdle()
        val state = v.uiState.value as DashboardUiState.Success
        assertEquals(8, state.data.activeCameras)
    }

    @Test
    fun `syncStatus becomes Synced after successful load`() = runTest {
        val v = vm()
        advanceUntilIdle()
        assertEquals(SyncStatus.Synced, v.syncStatus.value)
    }

    @Test
    fun `lastSynced is populated with HH colon MM format after success`() = runTest {
        val v = vm()
        advanceUntilIdle()
        val synced = v.lastSynced.value
        assertTrue(synced.isNotEmpty(), "lastSynced should not be empty")
        assertTrue(
            synced.matches(Regex("\\d{2}:\\d{2}")),
            "Expected HH:MM format, got: '$synced'"
        )
    }

    // ── Error path ────────────────────────────────────────────────────────────

    @Test
    fun `uiState becomes Error when dashboard API returns error`() = runTest {
        val errorRepo = fakeCameraRepo(dashboardResult = ApiResult.Error("API down", 503))
        val v = vm(cameraRepo = errorRepo)
        advanceUntilIdle()
        assertIs<DashboardUiState.Error>(v.uiState.value)
    }

    @Test
    fun `error message propagates to uiState`() = runTest {
        val errorRepo = fakeCameraRepo(dashboardResult = ApiResult.Error("Server unavailable", 503))
        val v = vm(cameraRepo = errorRepo)
        advanceUntilIdle()
        val state = v.uiState.value as DashboardUiState.Error
        assertEquals("Server unavailable", state.message)
    }

    @Test
    fun `syncStatus becomes Error when dashboard API fails`() = runTest {
        val errorRepo = fakeCameraRepo(dashboardResult = ApiResult.Error("fail", 503))
        val v = vm(cameraRepo = errorRepo)
        advanceUntilIdle()
        assertEquals(SyncStatus.Error, v.syncStatus.value)
    }

    // ── retry() ───────────────────────────────────────────────────────────────

    @Test
    fun `retry reaches Success after initial error when next call succeeds`() = runTest {
        var callCount = 0
        val repo = object : CameraRepository {
            override fun observeCameras(siteId: String?): Flow<List<Camera>> = flowOf(emptyList())
            override fun observeCamera(cameraId: String): Flow<Camera?> = flowOf(null)
            override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> = ApiResult.Success(emptyList())
            override suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto> = ApiResult.Error("n/a", 501)
            override suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>> = ApiResult.Success(emptyList())
            override suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto> {
                // First 7-call batch (trend) + 1 today call → all fail; retry batch succeeds
                return if (++callCount <= 8) ApiResult.Error("first fail", 503)
                else ApiResult.Success(successDashboard)
            }
            override suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto> = ApiResult.Error("n/a", 501)
            override suspend fun createCamera(camera: Camera): String = ""
            override suspend fun updateCamera(camera: Camera) {}
            override suspend fun deleteCamera(cameraId: String) {}
            override suspend fun refresh() {}
        }
        val v = vm(cameraRepo = repo)
        advanceUntilIdle()
        assertIs<DashboardUiState.Error>(v.uiState.value)

        v.retry()
        advanceUntilIdle()
        assertIs<DashboardUiState.Success>(v.uiState.value)
        assertEquals(SyncStatus.Synced, v.syncStatus.value)
    }

    @Test
    fun `retry sets syncStatus to Syncing before fetch completes`() = runTest {
        val v = vm()
        advanceUntilIdle()
        assertEquals(SyncStatus.Synced, v.syncStatus.value)

        // After calling retry, syncStatus resets to Syncing immediately
        v.retry()
        assertEquals(SyncStatus.Syncing, v.syncStatus.value)
    }

    // ── selectTab ─────────────────────────────────────────────────────────────

    @Test
    fun `selectTab changes the active tab`() = runTest {
        val v = vm()
        v.selectTab(DashboardTab.Cameras)
        assertEquals(DashboardTab.Cameras, v.selectedTab.value)
        v.selectTab(DashboardTab.Alerts)
        assertEquals(DashboardTab.Alerts, v.selectedTab.value)
        v.selectTab(DashboardTab.Profile)
        assertEquals(DashboardTab.Profile, v.selectedTab.value)
        v.selectTab(DashboardTab.Home)
        assertEquals(DashboardTab.Home, v.selectedTab.value)
    }

    @Test
    fun `selectTab does not affect uiState`() = runTest {
        val v = vm()
        advanceUntilIdle()
        val before = v.uiState.value
        v.selectTab(DashboardTab.Alerts)
        assertEquals(before, v.uiState.value)
    }
}
