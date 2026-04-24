package crocalert.app

import crocalert.app.domain.repository.CameraRepository
import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto
import crocalert.app.shared.network.ApiResult
import crocalert.app.ui.cameras.CameraHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CameraHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeRepo(
        camera: Camera? = null,
        capturesResult: ApiResult<List<CaptureDto>> = ApiResult.Success(emptyList()),
        statsResult: ApiResult<CameraDailyStatsDto> = ApiResult.Error("no stats", 404),
    ) = object : CameraRepository {
        override fun observeCameras(siteId: String?): Flow<List<Camera>> = flowOf(emptyList())
        override fun observeCamera(cameraId: String): Flow<Camera?> = flowOf(camera)
        override suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>> = capturesResult
        override suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto> = statsResult
        override suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>> = ApiResult.Success(emptyList())
        override suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto> = ApiResult.Error("n/a", 501)
        override suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto> = ApiResult.Error("n/a", 501)
        override suspend fun createCamera(camera: Camera): String = ""
        override suspend fun updateCamera(camera: Camera) {}
        override suspend fun deleteCamera(cameraId: String) {}
        override suspend fun refresh() {}
    }

    private fun vm(repo: CameraRepository = fakeRepo()) =
        CameraHistoryViewModel(cameraId = "cam1", cameraName = "Camera 1", repository = repo)

    // ── Navigation: prevDay ───────────────────────────────────────────────────

    @Test
    fun `prevDay decrements selectedDate by one day and canGoNext becomes true`() = runTest {
        val v = vm()
        advanceUntilIdle()
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(tz)
        assertEquals(today, v.uiState.value.selectedDate)

        v.prevDay()
        advanceUntilIdle()

        assertEquals(today.minus(1, DateTimeUnit.DAY), v.uiState.value.selectedDate)
        assertTrue(v.uiState.value.canGoNext)
    }

    // ── Navigation: nextDay on today is a no-op ───────────────────────────────

    @Test
    fun `nextDay is a no-op when selectedDate is today`() = runTest {
        val v = vm()
        advanceUntilIdle()
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(tz)
        assertEquals(today, v.uiState.value.selectedDate)

        v.nextDay()
        advanceUntilIdle()

        assertEquals(today, v.uiState.value.selectedDate)
        assertFalse(v.uiState.value.canGoNext)
    }

    // ── Navigation: nextDay on a past date advances date ─────────────────────

    @Test
    fun `nextDay advances date and sets canGoNext correctly when not on today`() = runTest {
        val v = vm()
        advanceUntilIdle()
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(tz)

        v.prevDay()
        advanceUntilIdle()
        v.prevDay()
        advanceUntilIdle()
        assertEquals(today.minus(2, DateTimeUnit.DAY), v.uiState.value.selectedDate)

        v.nextDay()
        advanceUntilIdle()

        assertEquals(today.minus(1, DateTimeUnit.DAY), v.uiState.value.selectedDate)
        assertTrue(v.uiState.value.canGoNext)
    }

    // ── setExpectedPerDay coercion ────────────────────────────────────────────

    @Test
    fun `setExpectedPerDay(0) coerces to 1`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.setExpectedPerDay(0)
        advanceUntilIdle()
        assertEquals(1, v.uiState.value.expectedPerDay)
    }

    @Test
    fun `setExpectedPerDay(99) coerces to 48`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.setExpectedPerDay(99)
        advanceUntilIdle()
        assertEquals(48, v.uiState.value.expectedPerDay)
    }

    // ── Slot-derived counts when no server stats ──────────────────────────────

    @Test
    fun `past day with N captures and no server stats yields received=N missing=expectedPerDay-N`() = runTest {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(tz)
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val startMs = yesterday.atStartOfDayIn(tz).toEpochMilliseconds()

        val captures = listOf(
            CaptureDto(id = "c1", captureTime = startMs + 3_600_000L, driveUrl = "https://drive.google.com/c1"),
            CaptureDto(id = "c2", captureTime = startMs + 7_200_000L, driveUrl = "https://drive.google.com/c2"),
            CaptureDto(id = "c3", captureTime = startMs + 10_800_000L, driveUrl = "https://drive.google.com/c3"),
        )

        val repo = fakeRepo(
            capturesResult = ApiResult.Success(captures),
            statsResult = ApiResult.Error("no stats", 404),
        )
        val v = vm(repo)
        advanceUntilIdle()

        v.prevDay()
        advanceUntilIdle()

        val state = v.uiState.value
        val expected = state.expectedPerDay
        assertEquals(3, state.received)
        assertEquals(expected - 3, state.missing)
    }

    // ── Server stats override local slot counts ───────────────────────────────

    @Test
    fun `server stats win over local slot-derived counts for received and missing`() = runTest {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(tz)
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val startMs = yesterday.atStartOfDayIn(tz).toEpochMilliseconds()

        val captures = listOf(
            CaptureDto(id = "c1", captureTime = startMs + 3_600_000L, driveUrl = "https://drive.google.com/c1"),
        )
        val serverStats = CameraDailyStatsDto(
            cameraId = "cam1",
            date = yesterday.toString(),
            expectedImages = 24,
            receivedImages = 20,
            missingImages = 4,
            isActive = true,
        )

        val repo = fakeRepo(
            capturesResult = ApiResult.Success(captures),
            statsResult = ApiResult.Success(serverStats),
        )
        val v = vm(repo)
        advanceUntilIdle()

        v.prevDay()
        advanceUntilIdle()

        val state = v.uiState.value
        assertEquals(20, state.received)
        assertEquals(4, state.missing)
    }

    // ── Both APIs return Error → error state ─────────────────────────────────

    @Test
    fun `both getDailyStats and getCapturesByCamera returning Error sets error in state`() = runTest {
        val repo = fakeRepo(
            capturesResult = ApiResult.Error("network failure", 503),
            statsResult = ApiResult.Error("network failure", 503),
        )
        val v = vm(repo)
        advanceUntilIdle()

        v.prevDay()
        advanceUntilIdle()

        assertNotNull(v.uiState.value.error)
    }
}
