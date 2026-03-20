package crocalert.app.feature.alerts.presentation

import crocalert.app.domain.repository.AlertRepository
import crocalert.app.feature.alerts.data.AlertSampleData
import crocalert.app.feature.alerts.data.MockAlertRepository
import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for [AlertsViewModel].
 *
 * Each test creates an isolated [CoroutineScope] backed by the [runTest]
 * TestScheduler so virtual time is controlled without leaking into other tests.
 * Calling [CoroutineScope.cancel] at the end of each test cleans up without
 * affecting the surrounding [runTest] scope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Returns a repository that emits [alerts] immediately and completes. */
    private fun repoWith(alerts: List<Alert>): AlertRepository = object : AlertRepository {
        override fun observeAlerts(): Flow<List<Alert>> = flowOf(alerts)
        override fun observeAlert(id: String): Flow<Alert?> = flowOf(alerts.find { it.id == id })
        override suspend fun createAlert(alert: Alert): String = alert.id
        override suspend fun updateAlert(alert: Alert) {}
        override suspend fun deleteAlert(alertId: String) {}
    }

    /** Returns a repository whose [observeAlerts] flow throws [message]. */
    private fun errorRepo(message: String): AlertRepository = object : AlertRepository {
        override fun observeAlerts(): Flow<List<Alert>> = flow { error(message) }
        override fun observeAlert(id: String): Flow<Alert?> = flow { error(message) }
        override suspend fun createAlert(alert: Alert): String = alert.id
        override suspend fun updateAlert(alert: Alert) {}
        override suspend fun deleteAlert(alertId: String) {}
    }

    // ── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial uiState is Loading`() = runTest {
        // A flow that never emits keeps the ViewModel in the Loading state.
        val neverRepo = object : AlertRepository {
            override fun observeAlerts(): Flow<List<Alert>> = MutableStateFlow<List<Alert>>(emptyList()).let {
                flow { /* never emits */ }
            }
            override fun observeAlert(id: String): Flow<Alert?> = flow { }
            override suspend fun createAlert(alert: Alert) = alert.id
            override suspend fun updateAlert(alert: Alert) {}
            override suspend fun deleteAlert(alertId: String) {}
        }
        // Create VM scope as a child backed by the test scheduler; safe to cancel independently.
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = neverRepo, coroutineScope = vmScope)

        // Before the scheduler advances, the state must already be Loading
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
    fun `success state alerts are sorted newest-first`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        val timestamps = state.alerts.map { it.createdAt }
        assertEquals(timestamps.sortedDescending(), timestamps, "Alerts must be ordered by createdAt descending")
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

    // ── Filtering ────────────────────────────────────────────────────────────

    @Test
    fun `ALERTS filter shows only HIGH and CRITICAL alerts`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.ALERTS)

        val state = vm.uiState.value as AlertsUiState.Success
        assertTrue(
            state.alerts.all {
                it.priority == AlertPriority.HIGH || it.priority == AlertPriority.CRITICAL
            },
        )
        vmScope.cancel()
    }

    @Test
    fun `PRE_ALERTS filter shows only MEDIUM alerts`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.PRE_ALERTS)

        val state = vm.uiState.value as AlertsUiState.Success
        assertTrue(state.alerts.all { it.priority == AlertPriority.MEDIUM })
        vmScope.cancel()
    }

    @Test
    fun `INFO filter shows only LOW alerts`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.INFO)

        val state = vm.uiState.value as AlertsUiState.Success
        assertTrue(state.alerts.all { it.priority == AlertPriority.LOW })
        vmScope.cancel()
    }

    @Test
    fun `switching back to ALL filter restores full list`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        vm.setFilter(AlertFilter.ALERTS)
        vm.setFilter(AlertFilter.ALL)

        val state = vm.uiState.value as AlertsUiState.Success
        assertEquals(AlertSampleData.alerts.size, state.alerts.size)
        vmScope.cancel()
    }

    // ── Unread logic ─────────────────────────────────────────────────────────

    @Test
    fun `unread alerts are present in Success state`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()

        val state = vm.uiState.value as AlertsUiState.Success
        assertTrue(state.alerts.any { !it.isRead }, "Expected at least one unread alert in success state")
        vmScope.cancel()
    }

    // ── Retry ────────────────────────────────────────────────────────────────

    @Test
    fun `retry re-loads and resolves back to Success`() = runTest {
        val vmScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val vm = AlertsViewModel(repository = MockAlertRepository(), coroutineScope = vmScope)
        advanceUntilIdle()
        assertIs<AlertsUiState.Success>(vm.uiState.value)

        vm.retry()
        advanceUntilIdle()
        assertIs<AlertsUiState.Success>(vm.uiState.value)
        vmScope.cancel()
    }
}
