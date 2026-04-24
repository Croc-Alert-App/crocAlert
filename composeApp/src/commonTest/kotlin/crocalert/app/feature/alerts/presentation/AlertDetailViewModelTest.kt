package crocalert.app.feature.alerts.presentation

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.domain.repository.CameraRepository
import crocalert.app.domain.repository.SiteRepository
import crocalert.app.model.Alert
import crocalert.app.model.Camera
import crocalert.app.model.Site
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AlertDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `load - emits Success with camera and site`() = runTest {
        val alert = Alert(id = "test-alert", cameraId = "cam1", captureId = "")
        val viewModel = vm(
            alertRepo = fakeAlertRepo(alert),
            cameraRepo = fakeCameraRepo(camera = Camera(id = "cam1", siteId = "site1")),
            siteRepo = fakeSiteRepo(site = Site(id = "site1")),
        )
        advanceUntilIdle()
        val state = assertIs<AlertDetailUiState.Success>(viewModel.uiState.value)
        assertNotNull(state.camera)
        assertNotNull(state.site)
    }

    @Test
    fun `load - emits Success with null camera when cameraId is blank`() = runTest {
        val alert = Alert(id = "test-alert", cameraId = "", captureId = "")
        val viewModel = vm(alertRepo = fakeAlertRepo(alert))
        advanceUntilIdle()
        val state = assertIs<AlertDetailUiState.Success>(viewModel.uiState.value)
        assertNull(state.camera)
        assertNull(state.site)
    }

    @Test
    fun `load - emits Success with null site when camera siteId is null`() = runTest {
        val alert = Alert(id = "test-alert", cameraId = "cam1", captureId = "")
        val viewModel = vm(
            alertRepo = fakeAlertRepo(alert),
            cameraRepo = fakeCameraRepo(camera = Camera(id = "cam1", siteId = null)),
        )
        advanceUntilIdle()
        val state = assertIs<AlertDetailUiState.Success>(viewModel.uiState.value)
        assertNotNull(state.camera)
        assertNull(state.site)
    }

    @Test
    fun `load - emits Success with null capture when captureId is blank`() = runTest {
        val alert = Alert(id = "test-alert", cameraId = "cam1", captureId = "")
        val viewModel = vm(
            alertRepo = fakeAlertRepo(alert),
            cameraRepo = fakeCameraRepo(
                captures = ApiResult.Success(listOf(CaptureDto(id = "cap1"))),
            ),
        )
        advanceUntilIdle()
        val state = assertIs<AlertDetailUiState.Success>(viewModel.uiState.value)
        assertNull(state.capture)
    }

    @Test
    fun `load - emits Success with matching capture found`() = runTest {
        val alert = Alert(id = "test-alert", cameraId = "cam1", captureId = "cap1")
        val viewModel = vm(
            alertRepo = fakeAlertRepo(alert),
            cameraRepo = fakeCameraRepo(
                captures = ApiResult.Success(listOf(CaptureDto(id = "cap1"), CaptureDto(id = "cap2"))),
            ),
        )
        advanceUntilIdle()
        val state = assertIs<AlertDetailUiState.Success>(viewModel.uiState.value)
        assertNotNull(state.capture)
        assertEquals("cap1", state.capture?.id)
    }

    @Test
    fun `load - emits Success with null capture when getCapturesByCamera returns Error`() = runTest {
        val alert = Alert(id = "test-alert", cameraId = "cam1", captureId = "cap1")
        val viewModel = vm(
            alertRepo = fakeAlertRepo(alert),
            cameraRepo = fakeCameraRepo(captures = ApiResult.Error("fail", 500)),
        )
        advanceUntilIdle()
        val state = assertIs<AlertDetailUiState.Success>(viewModel.uiState.value)
        assertNull(state.capture)
    }

    @Test
    fun `load - emits Error when observeAlert throws`() = runTest {
        val throwingRepo = object : AlertRepository {
            override val lastRefreshError: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
            override fun observeAlerts(): Flow<List<Alert>> = flowOf(emptyList())
            override fun observeAlert(alertId: String): Flow<Alert?> = flow { throw Exception("Network error") }
            override suspend fun createAlert(alert: Alert): String = alert.id
            override suspend fun updateAlert(alert: Alert) = Unit
            override suspend fun deleteAlert(alertId: String) = Unit
            override suspend fun refresh() = Unit
        }
        val viewModel = vm(alertRepo = throwingRepo)
        advanceUntilIdle()
        assertIs<AlertDetailUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `retry - resolves back to Success`() = runTest {
        val alert = Alert(id = "test-alert", cameraId = "cam1", captureId = "")
        val viewModel = vm(alertRepo = fakeAlertRepo(alert))
        advanceUntilIdle()
        assertIs<AlertDetailUiState.Success>(viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()
        assertIs<AlertDetailUiState.Success>(viewModel.uiState.value)
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private fun fakeAlertRepo(alert: Alert) = object : AlertRepository {
        private val alertFlow = MutableStateFlow<Alert?>(alert)
        override val lastRefreshError: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
        override fun observeAlerts(): Flow<List<Alert>> = flowOf(listOf(alert))
        override fun observeAlert(alertId: String): Flow<Alert?> = alertFlow.asStateFlow()
        override suspend fun createAlert(alert: Alert): String = alert.id
        override suspend fun updateAlert(alert: Alert) = Unit
        override suspend fun deleteAlert(alertId: String) = Unit
        override suspend fun refresh() = Unit
    }

    private fun fakeCameraRepo(
        camera: Camera? = Camera(id = "cam1", siteId = "site1"),
        captures: ApiResult<List<CaptureDto>> = ApiResult.Success(emptyList()),
    ) = object : CameraRepository {
        override fun observeCamera(cameraId: String): Flow<Camera?> = flowOf(camera)
        override fun observeCameras(siteId: String?): Flow<List<Camera>> = flowOf(emptyList())
        override suspend fun getCapturesByCamera(cameraId: String) = captures
        override suspend fun getDailyStats(cameraId: String, date: String) = ApiResult.Error("", 0)
        override suspend fun getDailyStatsForAll(date: String) = ApiResult.Error("", 0)
        override suspend fun getMonitoringDashboard(date: String) = ApiResult.Error("", 0)
        override suspend fun getGlobalCaptureRate(date: String) = ApiResult.Error("", 0)
        override suspend fun createCamera(camera: Camera): String = ""
        override suspend fun updateCamera(camera: Camera) = Unit
        override suspend fun deleteCamera(cameraId: String) = Unit
        override suspend fun refresh() = Unit
    }

    private fun fakeSiteRepo(
        site: Site? = Site(id = "site1"),
    ) = object : SiteRepository {
        override fun observeSite(siteId: String): Flow<Site?> = flowOf(site)
        override fun observeSites(): Flow<List<Site>> = flowOf(emptyList())
        override suspend fun createSite(site: Site): String = site.id
        override suspend fun updateSite(site: Site) = Unit
        override suspend fun deleteSite(siteId: String) = Unit
    }

    private fun vm(
        alertId: String = "test-alert",
        alertRepo: AlertRepository = fakeAlertRepo(Alert(id = "test-alert", cameraId = "cam1", captureId = "")),
        cameraRepo: CameraRepository = fakeCameraRepo(),
        siteRepo: SiteRepository = fakeSiteRepo(),
    ) = AlertDetailViewModel(alertId, alertRepo, cameraRepo, siteRepo)
}
