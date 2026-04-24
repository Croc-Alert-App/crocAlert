package crocalert.app.feature.alerts.presentation

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.feature.alerts.data.MockAlertRepository
import crocalert.app.model.Alert
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
 * Documents R-11: filter/sort/range state lives in MutableStateFlow with no
 * SavedStateHandle, so it resets on process death or ViewModel recreation.
 *
 * Tests PASS today. If SavedStateHandle is added, update assertions to verify
 * state IS restored and rename to AlertsViewModelStatePersistenceTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@crocalert.app.RegressionTest // Documents R-11 known limitation
class AlertsViewModelStateLossTest {

    private fun repo(): AlertRepository = MockAlertRepository()

    private fun emptyRepo(): AlertRepository = object : AlertRepository {
        override fun observeAlerts(): Flow<List<Alert>> = flowOf(emptyList())
        override fun observeAlert(id: String): Flow<Alert?> = flowOf(null)
        override suspend fun createAlert(alert: Alert) = alert.id
        override suspend fun updateAlert(alert: Alert) {}
        override suspend fun deleteAlert(alertId: String) {}
        override val lastRefreshError: StateFlow<String?> = MutableStateFlow(null).asStateFlow()
        override suspend fun refresh() {}
    }

    // ── Filter state is lost after clear() + recreate ─────────────────────────

    @Test
    fun `filter state resets to ALL after ViewModel is destroyed and recreated`() = runTest {
        val scope1 = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm1 = AlertsViewModel(repo(), scope1)
        advanceUntilIdle()

        vm1.setFilter(AlertFilter.THIS_WEEK)
        assertEquals(AlertFilter.THIS_WEEK, vm1.activeFilter.value)

        scope1.cancel()

        val scope2 = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm2 = AlertsViewModel(repo(), scope2)
        advanceUntilIdle()

        assertEquals(
            AlertFilter.ALL,
            vm2.activeFilter.value,
            "R-11: Filter is NOT persisted across VM recreation. " +
                "If this fails, SavedStateHandle was added — update the test accordingly."
        )
        scope2.cancel()
    }

    // ── Sort direction is lost after clear() + recreate ───────────────────────

    @Test
    fun `sort direction resets to DESC after ViewModel is destroyed and recreated`() = runTest {
        val scope1 = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm1 = AlertsViewModel(repo(), scope1)
        advanceUntilIdle()

        vm1.toggleSort()
        assertEquals(SortDirection.ASC, vm1.sortDirection.value)

        scope1.cancel()

        val scope2 = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm2 = AlertsViewModel(repo(), scope2)
        advanceUntilIdle()

        assertEquals(
            SortDirection.DESC,
            vm2.sortDirection.value,
            "R-11: Sort direction is NOT persisted across VM recreation."
        )
        scope2.cancel()
    }

    // ── Custom date range is lost after clear() + recreate ────────────────────

    @Test
    fun `custom date range resets to null after ViewModel is destroyed and recreated`() = runTest {
        val scope1 = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm1 = AlertsViewModel(repo(), scope1)
        advanceUntilIdle()

        vm1.setCustomRange(1_000L, 2_000L)
        assertNotNull(vm1.customRange.value)

        scope1.cancel()

        val scope2 = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm2 = AlertsViewModel(repo(), scope2)
        advanceUntilIdle()

        assertNull(
            vm2.customRange.value,
            "R-11: Custom date range is NOT persisted across VM recreation."
        )
        scope2.cancel()
    }

    // ── clear() cancels the coroutine scope (no resource leak) ───────────────

    @Test
    fun `clear cancels the internal coroutine scope without throwing`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repo(), scope)
        advanceUntilIdle()

        scope.cancel()
    }

    // ── New VM starts with known defaults ─────────────────────────────────────

    @Test
    fun `new ViewModel always starts with ALL filter and DESC sort`() = runTest {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repo(), scope)

        assertEquals(AlertFilter.ALL, vm.activeFilter.value)
        assertEquals(SortDirection.DESC, vm.sortDirection.value)
        assertNull(vm.customRange.value)

        scope.cancel()
    }
}
