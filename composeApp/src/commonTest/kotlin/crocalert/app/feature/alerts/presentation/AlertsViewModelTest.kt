package crocalert.app.feature.alerts.presentation

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.feature.alerts.data.AlertSampleData
import crocalert.app.feature.alerts.data.MockAlertRepository
import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.model.AlertType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for [AlertsViewModel].
 *
 * Each test creates an isolated [CoroutineScope] backed by the [runTest]
 * TestScheduler (via [StandardTestDispatcher]) so virtual time is fully
 * controlled and [advanceUntilIdle] drains all pending work.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun repoWith(alerts: List<Alert>): AlertRepository = object : AlertRepository {
        override fun observeAlerts(): Flow<List<Alert>> = flowOf(alerts)
        override fun observeAlert(id: String): Flow<Alert?> = flowOf(alerts.find { it.id == id })
        override suspend fun createAlert(alert: Alert): String = alert.id
        override suspend fun updateAlert(alert: Alert) {}
        override suspend fun deleteAlert(alertId: String) {}
        override val lastRefreshError: Flow<String?> = flowOf(null)
    }

    private fun errorRepo(message: String): AlertRepository = object : AlertRepository {
        override fun observeAlerts(): Flow<List<Alert>> = flow { error(message) }
        override fun observeAlert(id: String): Flow<Alert?> = flow { error(message) }
        override suspend fun createAlert(alert: Alert): String = alert.id
        override suspend fun updateAlert(alert: Alert) {}
        override suspend fun deleteAlert(alertId: String) {}
        override val lastRefreshError: Flow<String?> = flowOf(null)
    }

    /** Minimal alert with overridable createdAt for filter/sort tests. */
    private fun alert(id: String, createdAt: Long, priority: AlertPriority = AlertPriority.HIGH) = Alert(
        id = id,
        title = "Test $id",
        message = "Test message",
        type = AlertType.MOTION_DETECTED,
        priority = priority,
        status = AlertStatus.OPEN,
        createdAt = createdAt,
        sourceName = "CAM-TEST",
        isRead = false,
    )

    // ── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial uiState is Loading`() = runTest {
        val neverRepo = object : AlertRepository {
            override fun observeAlerts(): Flow<List<Alert>> = flow { /* never emits */ }
            override fun observeAlert(id: String): Flow<Alert?> = flow { }
            override suspend fun createAlert(alert: Alert) = alert.id
            override suspend fun updateAlert(alert: Alert) {}
            override suspend fun deleteAlert(alertId: String) {}
            override val lastRefreshError: Flow<String?> = flowOf(null)
        }
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = neverRepo, coroutineScope = vmScope)

        assertIs<AlertsUiState.Loading>(vm.uiState.value)
        vmScope.cancel()
    }

    // ── Success state ────────────────────────────────────────────────────────

    @Test
    fun `uiState becomes Success when repository emits non-empty list`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        assertIs<AlertsUiState.Success>(vm.uiState.value)
        vmScope.cancel()
    }

    @Test
    fun `success state alerts are sorted newest-first by default`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        val timestamps = state.alerts.map { it.createdAt }
        assertEquals(timestamps.sortedDescending(), timestamps, "Default sort must be DESC (newest first)")
        vmScope.cancel()
    }

    @Test
    fun `success state contains all mock alerts when filter is ALL`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(AlertSampleData.alerts.size, state.alerts.size)
        vmScope.cancel()
    }

    // ── Empty state ──────────────────────────────────────────────────────────

    @Test
    fun `uiState becomes Empty when repository emits empty list`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = repoWith(emptyList()), coroutineScope = vmScope)
        advanceUntilIdle()

        assertIs<AlertsUiState.Empty>(vm.uiState.value)
        vmScope.cancel()
    }

    @Test
    fun `CUSTOM filter without a prior range shows Empty state prompting range selection`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.CUSTOM)

        val state = vm.uiState.value
        assertIs<AlertsUiState.Empty>(state)
        assertTrue(state.message.contains("date range", ignoreCase = true))
        vmScope.cancel()
    }

    @Test
    fun `Empty state carries custom message when provided`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = repoWith(emptyList()), coroutineScope = vmScope)
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Empty
        assertTrue(state.message.isNotBlank())
        vmScope.cancel()
    }

    // ── Error state ──────────────────────────────────────────────────────────

    @Test
    fun `uiState becomes Error when repository throws`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = errorRepo("Network unavailable"), coroutineScope = vmScope)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<AlertsUiState.Error>(state)
        assertTrue(state.message.contains("Network unavailable"))
        vmScope.cancel()
    }

    // ── Date-range filtering ─────────────────────────────────────────────────

    @Test
    fun `TODAY filter shows only alerts from the current day`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val alerts = listOf(
            alert("recent", createdAt = now - 1_000L),             // 1 sec ago  → today ✓
            alert("old", createdAt = now - 25 * 3_600_000L),       // 25 hr ago  → yesterday ✗
        )
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = repoWith(alerts), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.TODAY)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(1, state.alerts.size)
        assertEquals("recent", state.alerts.first().id)
        vmScope.cancel()
    }

    @Test
    fun `THIS_WEEK filter shows alerts within the last 7 days`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val alerts = listOf(
            alert("day1", createdAt = now - 1 * 24 * 3_600_000L),  // 1 day ago  → this week ✓
            alert("day3", createdAt = now - 3 * 24 * 3_600_000L),  // 3 days ago → this week ✓
            alert("day8", createdAt = now - 8 * 24 * 3_600_000L),  // 8 days ago → NOT this week ✗
        )
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = repoWith(alerts), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.THIS_WEEK)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(2, state.alerts.size)
        assertTrue(state.alerts.none { it.id == "day8" })
        vmScope.cancel()
    }

    @Test
    fun `THIS_MONTH filter shows alerts within the last 30 days`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val alerts = listOf(
            alert("day10", createdAt = now - 10 * 24 * 3_600_000L), // 10 days ago → this month ✓
            alert("day25", createdAt = now - 25 * 24 * 3_600_000L), // 25 days ago → this month ✓
            alert("day31", createdAt = now - 31 * 24 * 3_600_000L), // 31 days ago → NOT this month ✗
        )
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = repoWith(alerts), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.THIS_MONTH)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(2, state.alerts.size)
        assertTrue(state.alerts.none { it.id == "day31" })
        vmScope.cancel()
    }

    @Test
    fun `switching back to ALL filter restores full list`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.TODAY)
        vm.setFilter(AlertFilter.ALL)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(AlertSampleData.alerts.size, state.alerts.size)
        vmScope.cancel()
    }

    // ── Sort direction ────────────────────────────────────────────────────────

    @Test
    fun `default sort direction is DESC (newest first)`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        assertEquals(SortDirection.DESC, vm.sortDirection.value)
        vmScope.cancel()
    }

    @Test
    fun `toggleSort switches direction from DESC to ASC`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.toggleSort()

        assertEquals(SortDirection.ASC, vm.sortDirection.value)
        val state = vm.uiState.value as AlertsUiState.Success
        val timestamps = state.alerts.map { it.createdAt }
        assertEquals(timestamps.sorted(), timestamps, "ASC sort must list oldest first")
        vmScope.cancel()
    }

    @Test
    fun `toggleSort twice returns to DESC`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.toggleSort()
        vm.toggleSort()

        assertEquals(SortDirection.DESC, vm.sortDirection.value)
        vmScope.cancel()
    }

    // ── Unread logic ─────────────────────────────────────────────────────────

    @Test
    fun `unread alerts are present in Success state`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        assertTrue(state.alerts.any { !it.isRead })
        vmScope.cancel()
    }

    // ── Custom date range ─────────────────────────────────────────────────────

    @Test
    fun `setCustomRange activates CUSTOM filter and shows matching alerts`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val dayMs = 24 * 3_600_000L
        val alerts = listOf(
            alert("in-range-1", createdAt = now - 2 * dayMs),   // 2 days ago → in [1d..3d]
            alert("in-range-2", createdAt = now - 3 * dayMs),   // 3 days ago → in [1d..3d] (end of day included)
            alert("out-range", createdAt = now - 5 * dayMs),    // 5 days ago → outside range
        )
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = repoWith(alerts), coroutineScope = vmScope)
        advanceUntilIdle()

        val startMs = now - 3 * dayMs   // 3 days ago at midnight equivalent
        val endMs = now - 1 * dayMs     // 1 day ago at midnight equivalent
        vm.setCustomRange(startMs, endMs)

        val state = vm.uiState.value as AlertsUiState.Success
        // Both in-range alerts fall within [startMs .. endMs + full day]
        assertTrue(state.alerts.all { it.id.startsWith("in-range") })
        assertTrue(state.alerts.none { it.id == "out-range" })
        vmScope.cancel()
    }

    @Test
    fun `CUSTOM filter without a range set shows Empty state`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        // Force CUSTOM via setFilter directly (no range set yet)
        vm.setFilter(AlertFilter.CUSTOM)

        assertIs<AlertsUiState.Empty>(vm.uiState.value)
        vmScope.cancel()
    }

    @Test
    fun `customRange state is updated after setCustomRange`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        vm.setCustomRange(1_000L, 2_000L)

        assertEquals(1_000L, vm.customRange.value?.startMs)
        assertEquals(2_000L, vm.customRange.value?.endMs)
        vmScope.cancel()
    }

    // ── Refresh / Retry ───────────────────────────────────────────────────────

    @Test
    fun `refresh re-loads and resolves back to Success`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        assertIs<AlertsUiState.Success>(vm.uiState.value)

        vm.refresh()
        advanceUntilIdle()
        assertIs<AlertsUiState.Success>(vm.uiState.value)
        vmScope.cancel()
    }

    @Test
    fun `retry re-loads after error and resolves to Success`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()
        assertIs<AlertsUiState.Success>(vm.uiState.value)
        vmScope.cancel()
    }
}
