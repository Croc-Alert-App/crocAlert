package crocalert.app

import crocalert.app.ui.dashboard.DashboardData
import crocalert.app.ui.dashboard.DashboardTab
import crocalert.app.ui.dashboard.DashboardUiState
import crocalert.app.ui.dashboard.DashboardViewModel
import crocalert.app.ui.dashboard.SyncStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

/**
 * Unit tests for [DashboardViewModel].
 *
 * DashboardViewModel accepts a `loadDashboard: suspend () -> DashboardData` lambda,
 * making it trivially testable: pass a lambda that returns data, throws, or suspends
 * indefinitely (via CompletableDeferred) to control timing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val successData = DashboardData(
        activeCameras = 5,
        networkHealthPct = 0.95f,
        activeAlerts = 2,
        criticalAlerts = 1,
        captureRate = "98%",
        captureRatePct = 0.98f,
        integrityPct = 0.97f,
        networkTrend = emptyList(),
        metadataMetrics = emptyList(),
        recentActivity = emptyList()
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial / loading state ───────────────────────────────────────────────

    @Test
    fun `initial uiState is Loading before data arrives`() = runTest {
        val deferred = CompletableDeferred<DashboardData>()
        val vm = DashboardViewModel(loadDashboard = { deferred.await() })
        // loadData() launched; suspended at deferred.await() — state not yet resolved
        assertEquals(DashboardUiState.Loading, vm.uiState.value)
        deferred.cancel()
    }

    @Test
    fun `initial syncStatus is Syncing before data arrives`() = runTest {
        val deferred = CompletableDeferred<DashboardData>()
        val vm = DashboardViewModel(loadDashboard = { deferred.await() })
        assertEquals(SyncStatus.Syncing, vm.syncStatus.value)
        deferred.cancel()
    }

    // ── Success path ──────────────────────────────────────────────────────────

    @Test
    fun `uiState becomes Success after loadDashboard completes`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { successData })
        advanceUntilIdle()
        assertIs<DashboardUiState.Success>(vm.uiState.value)
        assertEquals(5, (vm.uiState.value as DashboardUiState.Success).data.activeCameras)
    }

    @Test
    fun `syncStatus becomes Synced after successful load`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { successData })
        advanceUntilIdle()
        assertEquals(SyncStatus.Synced, vm.syncStatus.value)
    }

    @Test
    fun `lastSynced is populated with HH colon MM format after success`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { successData })
        advanceUntilIdle()
        val synced = vm.lastSynced.value
        assertTrue(synced.isNotEmpty(), "lastSynced should not be empty")
        assertTrue(
            synced.matches(Regex("\\d{2}:\\d{2}")),
            "Expected HH:MM format, got: '$synced'"
        )
    }

    // ── Error path ────────────────────────────────────────────────────────────

    @Test
    fun `uiState becomes Error when loadDashboard throws`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { throw RuntimeException("API down") })
        advanceUntilIdle()
        val state = vm.uiState.value
        assertIs<DashboardUiState.Error>(state)
        assertEquals("API down", state.message)
    }

    @Test
    fun `syncStatus becomes Error when loadDashboard throws`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { throw RuntimeException("fail") })
        advanceUntilIdle()
        assertEquals(SyncStatus.Error, vm.syncStatus.value)
    }

    @Test
    fun `Error message falls back to localised string when exception message is null`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { throw RuntimeException(null as String?) })
        advanceUntilIdle()
        val state = vm.uiState.value
        assertIs<DashboardUiState.Error>(state)
        assertEquals("Error desconocido", state.message)
    }

    // ── retry() ───────────────────────────────────────────────────────────────

    @Test
    fun `retry resets to Loading then reaches Success`() = runTest {
        var shouldFail = true
        val vm = DashboardViewModel(loadDashboard = {
            if (shouldFail) throw RuntimeException("first fail") else successData
        })
        advanceUntilIdle()
        assertIs<DashboardUiState.Error>(vm.uiState.value)

        shouldFail = false
        vm.retry()
        advanceUntilIdle()
        assertIs<DashboardUiState.Success>(vm.uiState.value)
        assertEquals(SyncStatus.Synced, vm.syncStatus.value)
    }

    @Test
    fun `retry sets syncStatus to Syncing before fetch completes`() = runTest {
        val firstLoad = CompletableDeferred<DashboardData>()
        firstLoad.complete(successData)   // first load succeeds immediately
        var callCount = 0
        val secondDeferred = CompletableDeferred<DashboardData>()

        val vm = DashboardViewModel(loadDashboard = {
            if (++callCount == 1) firstLoad.await() else secondDeferred.await()
        })
        advanceUntilIdle()
        assertEquals(SyncStatus.Synced, vm.syncStatus.value)

        vm.retry()  // sets Syncing synchronously, then suspends on secondDeferred.await()
        assertEquals(SyncStatus.Syncing, vm.syncStatus.value)
        assertEquals(DashboardUiState.Loading, vm.uiState.value)

        secondDeferred.complete(successData)
        advanceUntilIdle()
        assertEquals(SyncStatus.Synced, vm.syncStatus.value)
    }

    @Test
    fun `retry reaches Error when loadDashboard fails again`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { throw RuntimeException("always fail") })
        advanceUntilIdle()
        assertIs<DashboardUiState.Error>(vm.uiState.value)

        vm.retry()
        advanceUntilIdle()
        assertIs<DashboardUiState.Error>(vm.uiState.value)
        assertEquals(SyncStatus.Error, vm.syncStatus.value)
    }

    // ── selectTab ─────────────────────────────────────────────────────────────

    @Test
    fun `selectTab changes the active tab`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { successData })
        vm.selectTab(DashboardTab.Cameras)
        assertEquals(DashboardTab.Cameras, vm.selectedTab.value)
        vm.selectTab(DashboardTab.Alerts)
        assertEquals(DashboardTab.Alerts, vm.selectedTab.value)
        vm.selectTab(DashboardTab.Profile)
        assertEquals(DashboardTab.Profile, vm.selectedTab.value)
        vm.selectTab(DashboardTab.Home)
        assertEquals(DashboardTab.Home, vm.selectedTab.value)
    }

    @Test
    fun `selectTab does not affect uiState`() = runTest {
        val vm = DashboardViewModel(loadDashboard = { successData })
        advanceUntilIdle()
        val before = vm.uiState.value
        vm.selectTab(DashboardTab.Alerts)
        assertEquals(before, vm.uiState.value)
    }
}
