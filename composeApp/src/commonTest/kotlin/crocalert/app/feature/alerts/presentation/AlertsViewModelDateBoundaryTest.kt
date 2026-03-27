package crocalert.app.feature.alerts.presentation

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.model.AlertType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Boundary tests for [AlertsViewModel] date filters using a pinned [Clock].
 *
 * "Now" is fixed at 2026-03-19T12:00:00Z (noon UTC) so every boundary
 * calculation is deterministic regardless of the machine's timezone or the
 * time the test suite runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelDateBoundaryTest {

    // ── Fixed clock ───────────────────────────────────────────────────────────

    /** 2026-03-19T12:00:00Z in epoch-millis. */
    private val fixedNowMs = 1_742_385_600_000L

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(fixedNowMs)
    }

    // ── Constants (must mirror AlertsViewModel companion) ─────────────────────

    private val DAY_MS = 24 * 3_600_000L
    private val SEVEN_DAYS_MS = 7 * DAY_MS
    private val THIRTY_DAYS_MS = 30 * DAY_MS

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun repoWith(alerts: List<Alert>): AlertRepository = object : AlertRepository {
        override fun observeAlerts(): Flow<List<Alert>> = flowOf(alerts)
        override fun observeAlert(id: String): Flow<Alert?> = flowOf(alerts.find { it.id == id })
        override suspend fun createAlert(alert: Alert): String = alert.id
        override suspend fun updateAlert(alert: Alert) {}
        override suspend fun deleteAlert(alertId: String) {}
        override val lastRefreshError: StateFlow<String?> = MutableStateFlow(null).asStateFlow()
        override suspend fun refresh() {}
    }

    private fun alert(id: String, createdAt: Long) = Alert(
        id = id,
        title = "Boundary $id",
        message = "",
        type = AlertType.MOTION_DETECTED,
        priority = AlertPriority.LOW,
        status = AlertStatus.OPEN,
        createdAt = createdAt,
        sourceName = "CAM-BOUNDARY",
        isRead = false,
    )

    private fun viewModel(alerts: List<Alert>, scope: CoroutineScope) =
        AlertsViewModel(
            repository = repoWith(alerts),
            coroutineScope = scope,
            clock = fixedClock,
        )

    // ── THIS_WEEK boundaries ──────────────────────────────────────────────────

    @Test
    fun `THIS_WEEK - alert at exactly now minus 7 days is included`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val atBoundary = fixedNowMs - SEVEN_DAYS_MS   // == now - 7d  → included (>=)
        val vm = viewModel(listOf(alert("boundary", atBoundary)), scope)
        advanceUntilIdle()

        vm.setFilter(AlertFilter.THIS_WEEK)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(1, state.alerts.size)
        assertEquals("boundary", state.alerts.first().id)
        scope.cancel()
    }

    @Test
    fun `THIS_WEEK - alert 1ms before the 7-day boundary is excluded`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val justOutside = fixedNowMs - SEVEN_DAYS_MS - 1L   // one ms too old
        val vm = viewModel(listOf(alert("outside", justOutside)), scope)
        advanceUntilIdle()

        vm.setFilter(AlertFilter.THIS_WEEK)

        assertIs<AlertsUiState.Empty>(vm.uiState.value)
        scope.cancel()
    }

    @Test
    fun `THIS_WEEK - alert 1ms after the 7-day boundary is included`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val justInside = fixedNowMs - SEVEN_DAYS_MS + 1L
        val vm = viewModel(listOf(alert("inside", justInside)), scope)
        advanceUntilIdle()

        vm.setFilter(AlertFilter.THIS_WEEK)

        assertIs<AlertsUiState.Success>(vm.uiState.value)
        scope.cancel()
    }

    // ── THIS_MONTH boundaries ─────────────────────────────────────────────────

    @Test
    fun `THIS_MONTH - alert at exactly now minus 30 days is included`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val atBoundary = fixedNowMs - THIRTY_DAYS_MS
        val vm = viewModel(listOf(alert("boundary", atBoundary)), scope)
        advanceUntilIdle()

        vm.setFilter(AlertFilter.THIS_MONTH)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(1, state.alerts.size)
        scope.cancel()
    }

    @Test
    fun `THIS_MONTH - alert 1ms before the 30-day boundary is excluded`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val justOutside = fixedNowMs - THIRTY_DAYS_MS - 1L
        val vm = viewModel(listOf(alert("outside", justOutside)), scope)
        advanceUntilIdle()

        vm.setFilter(AlertFilter.THIS_MONTH)

        assertIs<AlertsUiState.Empty>(vm.uiState.value)
        scope.cancel()
    }

    // ── CUSTOM range boundaries ───────────────────────────────────────────────

    @Test
    fun `CUSTOM - alert at exact startMs boundary is included`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val startMs = fixedNowMs - 5 * DAY_MS
        val endMs = fixedNowMs - 1 * DAY_MS
        val vm = viewModel(listOf(alert("at-start", startMs)), scope)
        advanceUntilIdle()

        vm.setCustomRange(startMs, endMs)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(1, state.alerts.size)
        assertEquals("at-start", state.alerts.first().id)
        scope.cancel()
    }

    @Test
    fun `CUSTOM - alert at endMs plus full day minus 1ms is included (end-of-day inclusive)`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val startMs = fixedNowMs - 5 * DAY_MS
        val endMs = fixedNowMs - 1 * DAY_MS
        // CUSTOM logic: filter { it.createdAt in startMs..(endMs + DAY_MS - 1) }
        val endOfDay = endMs + DAY_MS - 1L
        val vm = viewModel(listOf(alert("end-of-day", endOfDay)), scope)
        advanceUntilIdle()

        vm.setCustomRange(startMs, endMs)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(1, state.alerts.size)
        scope.cancel()
    }

    @Test
    fun `CUSTOM - alert 1ms after the inclusive end boundary is excluded`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val startMs = fixedNowMs - 5 * DAY_MS
        val endMs = fixedNowMs - 1 * DAY_MS
        val justAfterEnd = endMs + DAY_MS   // endMs + DAY_MS - 1 is last included; +DAY_MS is out
        val vm = viewModel(listOf(alert("after-end", justAfterEnd)), scope)
        advanceUntilIdle()

        vm.setCustomRange(startMs, endMs)

        assertIs<AlertsUiState.Empty>(vm.uiState.value)
        scope.cancel()
    }

    @Test
    fun `CUSTOM - alert 1ms before startMs is excluded`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val startMs = fixedNowMs - 5 * DAY_MS
        val endMs = fixedNowMs - 1 * DAY_MS
        val beforeStart = startMs - 1L
        val vm = viewModel(listOf(alert("before-start", beforeStart)), scope)
        advanceUntilIdle()

        vm.setCustomRange(startMs, endMs)

        assertIs<AlertsUiState.Empty>(vm.uiState.value)
        scope.cancel()
    }

    // ── ALL filter with fixed clock ───────────────────────────────────────────

    @Test
    fun `ALL filter returns every alert regardless of age with fixed clock`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val alerts = listOf(
            alert("very-old", fixedNowMs - 365 * DAY_MS),
            alert("old", fixedNowMs - 60 * DAY_MS),
            alert("recent", fixedNowMs - 1_000L),
        )
        val vm = viewModel(alerts, scope)
        advanceUntilIdle()

        // Default filter is ALL
        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(3, state.alerts.size)
        scope.cancel()
    }

    // ── Sort order with fixed timestamps ──────────────────────────────────────

    @Test
    fun `DESC sort places newest alert first with pinned timestamps`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val alerts = listOf(
            alert("oldest", fixedNowMs - 3 * DAY_MS),
            alert("middle", fixedNowMs - 2 * DAY_MS),
            alert("newest", fixedNowMs - 1 * DAY_MS),
        )
        val vm = viewModel(alerts, scope)
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals("newest", state.alerts[0].id)
        assertEquals("oldest", state.alerts[2].id)
        scope.cancel()
    }

    @Test
    fun `ASC sort places oldest alert first with pinned timestamps`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val alerts = listOf(
            alert("oldest", fixedNowMs - 3 * DAY_MS),
            alert("middle", fixedNowMs - 2 * DAY_MS),
            alert("newest", fixedNowMs - 1 * DAY_MS),
        )
        val vm = viewModel(alerts, scope)
        advanceUntilIdle()
        vm.toggleSort()

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals("oldest", state.alerts[0].id)
        assertEquals("newest", state.alerts[2].id)
        scope.cancel()
    }

    // ── THIS_WEEK + THIS_MONTH mixed boundary ─────────────────────────────────

    @Test
    fun `THIS_WEEK excludes alerts visible under THIS_MONTH at 8-day mark`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val eightDaysAgo = fixedNowMs - 8 * DAY_MS   // week=no, month=yes
        val threeDaysAgo = fixedNowMs - 3 * DAY_MS   // week=yes, month=yes
        val vm = viewModel(
            listOf(alert("8d", eightDaysAgo), alert("3d", threeDaysAgo)),
            scope
        )
        advanceUntilIdle()

        vm.setFilter(AlertFilter.THIS_WEEK)
        val weekState = vm.uiState.value as AlertsUiState.Success
        assertEquals(1, weekState.alerts.size)
        assertEquals("3d", weekState.alerts.first().id)

        vm.setFilter(AlertFilter.THIS_MONTH)
        val monthState = vm.uiState.value as AlertsUiState.Success
        assertEquals(2, monthState.alerts.size)
        scope.cancel()
    }
}
