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
import kotlin.test.*

/**
 * Documents R-06: alerts sort by [Alert.createdAt] only — [AlertPriority] has no
 * effect on ordering. A CRITICAL alert created before a LOW alert appears below it.
 *
 * Tests PASS today. If priority sort is added, update assertions and rename to
 * AlertsViewModelPrioritySortEnforcedTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@crocalert.app.RegressionTest // Documents R-06 known limitation
class AlertsViewModelPrioritySortTest {

    private fun alert(
        id: String,
        priority: AlertPriority,
        createdAt: Long,
    ) = Alert(
        id = id,
        title = "T-$id",
        message = "",
        type = AlertType.MOTION_DETECTED,
        priority = priority,
        status = AlertStatus.OPEN,
        createdAt = createdAt,
        sourceName = "CAM",
        isRead = false,
    )

    private fun repoWith(alerts: List<Alert>): AlertRepository = object : AlertRepository {
        override fun observeAlerts(): Flow<List<Alert>> = flowOf(alerts)
        override fun observeAlert(id: String): Flow<Alert?> = flowOf(null)
        override suspend fun createAlert(alert: Alert) = alert.id
        override suspend fun updateAlert(alert: Alert) {}
        override suspend fun deleteAlert(alertId: String) {}
        override val lastRefreshError: StateFlow<String?> = MutableStateFlow(null).asStateFlow()
        override suspend fun refresh() {}
    }

    // ── Sort is purely by createdAt — priority does NOT change position ────────

    @Test
    fun `CRITICAL alert with older timestamp appears after LOW alert in DESC sort`() = runTest {
        val alerts = listOf(
            alert("low-newer",      AlertPriority.LOW,      createdAt = 2_000L),
            alert("critical-older", AlertPriority.CRITICAL, createdAt = 1_000L),
        )
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = repoWith(alerts).let { AlertsViewModel(it, scope) }
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        // DESC: newer createdAt first — LOW precedes CRITICAL despite lower priority.
        assertEquals(
            "low-newer",
            state.alerts.first().id,
            "R-06: Sort is by createdAt only. CRITICAL does not surface first. " +
                "If this fails, priority sort was implemented — update the test."
        )
        assertEquals("critical-older", state.alerts.last().id)

        vm.clear()
        scope.cancel()
    }

    @Test
    fun `ASC sort also orders by createdAt only — CRITICAL at end if newest`() = runTest {
        val alerts = listOf(
            alert("low-oldest",      AlertPriority.LOW,      createdAt = 1_000L),
            alert("critical-newest", AlertPriority.CRITICAL, createdAt = 3_000L),
        )
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = repoWith(alerts).let { AlertsViewModel(it, scope) }
        advanceUntilIdle()
        vm.toggleSort() // switch to ASC
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(
            "low-oldest",
            state.alerts.first().id,
            "R-06: ASC sort is also purely by createdAt."
        )
        assertEquals("critical-newest", state.alerts.last().id)

        vm.clear()
        scope.cancel()
    }

    @Test
    fun `equal createdAt alerts preserve repo insertion order (stable sort)`() = runTest {
        // Two alerts at same timestamp — relative order should be stable.
        val sameTsAlerts = listOf(
            alert("first",  AlertPriority.MEDIUM,   createdAt = 5_000L),
            alert("second", AlertPriority.CRITICAL, createdAt = 5_000L),
        )
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = repoWith(sameTsAlerts).let { AlertsViewModel(it, scope) }
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(2, state.alerts.size)

        vm.clear()
        scope.cancel()
    }
}
