package crocalert.app

import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto
import crocalert.app.shared.network.ApiResult
import crocalert.app.ui.cameras.CameraFilter
import crocalert.app.ui.cameras.CameraStatus
import crocalert.app.ui.cameras.CameraUiItem
import crocalert.app.ui.cameras.CamerasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

/**
 * Unit tests for [CamerasViewModel].
 *
 * Design note: filteredCameras and statusCounts use SharingStarted.WhileSubscribed,
 * so each test that checks their value must first start a collector — done via
 * `launch { vm.filteredCameras.collect { } }`. With UnconfinedTestDispatcher this
 * runs eagerly and the StateFlow reflects the computed value immediately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CamerasViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeCameraRepo = object : CameraRepository {
        override fun observeCameras(siteId: String?): Flow<List<Camera>> = flowOf(emptyList())
        override fun observeCamera(cameraId: String): Flow<Camera?> = flowOf(null)
        override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> = ApiResult.Success(emptyList())
        override suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto> = ApiResult.Error("not implemented", 501)
        override suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>> = ApiResult.Success(emptyList())
        override suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto> = ApiResult.Error("not implemented", 501)
        override suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto> = ApiResult.Error("not implemented", 501)
        override suspend fun createCamera(camera: Camera): String = ""
        override suspend fun updateCamera(camera: Camera) {}
        override suspend fun deleteCamera(cameraId: String) {}
        override suspend fun refresh() {}
    }

    // Three cameras: one per status, distinct names and IDs for search tests.
    private val alertCam     = camera("A", "Cam Alpha",   CameraStatus.Alert)
    private val okCam        = camera("B", "Cam Beta",    CameraStatus.Ok)
    private val attentionCam = camera("C", "Cam Gamma",   CameraStatus.Attention)
    private val allCameras   = listOf(alertCam, okCam, attentionCam)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = CamerasViewModel(cameraRepository = fakeCameraRepo, initialCameras = allCameras)

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial searchQuery is empty string`() = runTest {
        assertEquals("", vm().searchQuery.value)
    }

    @Test
    fun `initial selectedFilter is All`() = runTest {
        assertEquals(CameraFilter.All, vm().selectedFilter.value)
    }

    @Test
    fun `initial expandedCameraId is null`() = runTest {
        assertNull(vm().expandedCameraId.value)
    }

    // ── Filter behavior ───────────────────────────────────────────────────────

    @Test
    fun `filter All shows all cameras`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onFilterSelect(CameraFilter.All)
        advanceUntilIdle()
        assertEquals(3, v.filteredCameras.value.size)
        job.cancel()
    }

    @Test
    fun `filter Alert shows only Alert cameras`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onFilterSelect(CameraFilter.Alert)
        advanceUntilIdle()
        val result = v.filteredCameras.value
        assertEquals(1, result.size)
        assertEquals(CameraStatus.Alert, result[0].status)
        job.cancel()
    }

    @Test
    fun `filter Ok shows only Ok cameras`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onFilterSelect(CameraFilter.Ok)
        advanceUntilIdle()
        val result = v.filteredCameras.value
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.status == CameraStatus.Ok })
        job.cancel()
    }

    @Test
    fun `filter Attention shows only Attention cameras`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onFilterSelect(CameraFilter.Attention)
        advanceUntilIdle()
        val result = v.filteredCameras.value
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.status == CameraStatus.Attention })
        job.cancel()
    }

    // ── Search behavior ───────────────────────────────────────────────────────

    @Test
    fun `search by name is case-insensitive`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onSearchChange("alpha")          // lower-case should match "Cam Alpha"
        advanceUntilIdle()
        val result = v.filteredCameras.value
        assertEquals(1, result.size)
        assertEquals("Cam Alpha", result[0].name)
        job.cancel()
    }

    @Test
    fun `search by id matches camera`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onSearchChange("B")              // id of okCam
        advanceUntilIdle()
        assertTrue(v.filteredCameras.value.any { it.id == "B" })
        job.cancel()
    }

    @Test
    fun `search with no match returns empty list`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onSearchChange("ZZZNOTFOUND")
        advanceUntilIdle()
        assertTrue(v.filteredCameras.value.isEmpty())
        job.cancel()
    }

    @Test
    fun `empty search restores full camera list`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onSearchChange("Alpha")
        advanceUntilIdle()
        assertEquals(1, v.filteredCameras.value.size)
        v.onSearchChange("")
        advanceUntilIdle()
        assertEquals(3, v.filteredCameras.value.size)
        job.cancel()
    }

    // ── Combined filter + search ──────────────────────────────────────────────

    @Test
    fun `Alert filter and Alpha search returns matching Alert camera`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onFilterSelect(CameraFilter.Alert)
        v.onSearchChange("Alpha")
        advanceUntilIdle()
        val result = v.filteredCameras.value
        assertEquals(1, result.size)
        assertEquals("A", result[0].id)
        job.cancel()
    }

    @Test
    fun `Alert filter and Beta search returns empty because Beta is Ok`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onFilterSelect(CameraFilter.Alert)
        v.onSearchChange("Beta")
        advanceUntilIdle()
        assertTrue(v.filteredCameras.value.isEmpty())
        job.cancel()
    }

    // ── clearSearch ───────────────────────────────────────────────────────────

    @Test
    fun `clearSearch resets query to empty and filter to All`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        v.onFilterSelect(CameraFilter.Alert)
        v.onSearchChange("Alpha")
        advanceUntilIdle()

        v.clearSearch()
        advanceUntilIdle()

        assertEquals("", v.searchQuery.value)
        assertEquals(CameraFilter.All, v.selectedFilter.value)
        assertEquals(3, v.filteredCameras.value.size)
        job.cancel()
    }

    // ── toggleExpand ──────────────────────────────────────────────────────────

    @Test
    fun `toggleExpand sets expandedCameraId`() = runTest {
        val v = vm()
        v.toggleExpand("A")
        assertEquals("A", v.expandedCameraId.value)
    }

    @Test
    fun `toggleExpand same id collapses the card`() = runTest {
        val v = vm()
        v.toggleExpand("A")
        v.toggleExpand("A")
        assertNull(v.expandedCameraId.value)
    }

    @Test
    fun `toggleExpand different id switches to new card`() = runTest {
        val v = vm()
        v.toggleExpand("A")
        v.toggleExpand("B")
        assertEquals("B", v.expandedCameraId.value)
    }

    @Test
    fun `only one camera can be expanded at a time`() = runTest {
        val v = vm()
        v.toggleExpand("A")
        v.toggleExpand("C")
        // A should be collapsed, C should be open
        assertEquals("C", v.expandedCameraId.value)
    }

    // ── filteredCameras sorting ───────────────────────────────────────────────

    @Test
    fun `filteredCameras is sorted by status severity ascending`() = runTest {
        val v = vm()
        val job = launch { v.filteredCameras.collect { } }
        advanceUntilIdle()
        val severities = v.filteredCameras.value.map { it.status.severity }
        assertEquals(severities.sorted(), severities)
        job.cancel()
    }

    // ── statusCounts ──────────────────────────────────────────────────────────

    @Test
    fun `statusCounts reflects counts from the full unfiltered list`() = runTest {
        val v = vm()
        val job = launch { v.statusCounts.collect { } }
        advanceUntilIdle()
        val counts = v.statusCounts.value
        assertEquals(1, counts[CameraStatus.Alert])
        assertEquals(1, counts[CameraStatus.Ok])
        assertEquals(1, counts[CameraStatus.Attention])
        job.cancel()
    }

    @Test
    fun `statusCounts is not affected by active filter`() = runTest {
        val v = vm()
        val filterJob = launch { v.filteredCameras.collect { } }
        val countsJob = launch { v.statusCounts.collect { } }
        v.onFilterSelect(CameraFilter.Alert)      // UI shows only Alert cameras
        advanceUntilIdle()
        // But statusCounts still shows totals from all cameras
        val counts = v.statusCounts.value
        assertEquals(1, counts[CameraStatus.Ok])
        assertEquals(1, counts[CameraStatus.Attention])
        filterJob.cancel()
        countsJob.cancel()
    }

    // ── Background stats loading ──────────────────────────────────────────────

    @Test
    fun `stats are fetched in background after cameras are rendered`() = runTest {
        var statsCalled = false
        val repo = object : CameraRepository {
            override fun observeCameras(siteId: String?): Flow<List<Camera>> = flowOf(emptyList())
            override fun observeCamera(cameraId: String): Flow<Camera?> = flowOf(null)
            override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> = ApiResult.Success(emptyList())
            override suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto> = ApiResult.Error("n/a", 501)
            override suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>> {
                statsCalled = true
                return ApiResult.Success(emptyList())
            }
            override suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto> = ApiResult.Error("n/a", 501)
            override suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto> = ApiResult.Error("n/a", 501)
            override suspend fun createCamera(camera: Camera): String = ""
            override suspend fun updateCamera(camera: Camera) {}
            override suspend fun deleteCamera(cameraId: String) {}
            override suspend fun refresh() {}
        }
        // No initialCameras → loadData() runs and triggers getDailyStatsForAll
        val v = CamerasViewModel(cameraRepository = repo)
        val job = launch { v.filteredCameras.collect { } }
        advanceUntilIdle()
        assertFalse(v.isLoading.value)
        assertTrue(statsCalled)
        job.cancel()
    }

    @Test
    fun `stats error sets error message`() = runTest {
        val repo = object : CameraRepository {
            override fun observeCameras(siteId: String?): Flow<List<Camera>> = flowOf(emptyList())
            override fun observeCamera(cameraId: String): Flow<Camera?> = flowOf(null)
            override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> = ApiResult.Success(emptyList())
            override suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto> = ApiResult.Error("n/a", 501)
            override suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>> =
                ApiResult.Error("network error", 503)
            override suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto> = ApiResult.Error("n/a", 501)
            override suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto> = ApiResult.Error("n/a", 501)
            override suspend fun createCamera(camera: Camera): String = ""
            override suspend fun updateCamera(camera: Camera) {}
            override suspend fun deleteCamera(cameraId: String) {}
            override suspend fun refresh() {}
        }
        // No initialCameras → loadData() runs and reaches the error path
        val v = CamerasViewModel(cameraRepository = repo)
        val job = launch { v.filteredCameras.collect { } }
        advanceUntilIdle()
        assertNotNull(v.error.value)
        assertFalse(v.isLoading.value)
        job.cancel()
    }

    @Test
    fun `stats success does not produce an error`() = runTest {
        val stats = CameraDailyStatsDto(cameraId = "A", date = "2026-01-01", expectedImages = 10, receivedImages = 8, missingImages = 2, isActive = true)
        val repo = object : CameraRepository {
            override fun observeCameras(siteId: String?): Flow<List<Camera>> = flowOf(emptyList())
            override fun observeCamera(cameraId: String): Flow<Camera?> = flowOf(null)
            override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> = ApiResult.Success(emptyList())
            override suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto> = ApiResult.Error("n/a", 501)
            override suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>> =
                ApiResult.Success(listOf(stats))
            override suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto> = ApiResult.Error("n/a", 501)
            override suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto> = ApiResult.Error("n/a", 501)
            override suspend fun createCamera(camera: Camera): String = ""
            override suspend fun updateCamera(camera: Camera) {}
            override suspend fun deleteCamera(cameraId: String) {}
            override suspend fun refresh() {}
        }
        val v = CamerasViewModel(cameraRepository = repo)
        val job = launch { v.filteredCameras.collect { } }
        advanceUntilIdle()
        assertNull(v.error.value)
        assertFalse(v.isLoading.value)
        job.cancel()
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun camera(id: String, name: String, status: CameraStatus) = CameraUiItem(
        id = id,
        name = name,
        isActive = true,
        status = status,
        lastCapture = "1h ago",
        imagesSent = 10,
        imagesExpected = 10,
        captureCount = 10,
        captureExpected = 10,
        missingCaptures = 0,
        integrityFlags = 0
    )
}
