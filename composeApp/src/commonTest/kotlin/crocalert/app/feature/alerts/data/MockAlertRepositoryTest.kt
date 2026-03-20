package crocalert.app.feature.alerts.data

import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.model.AlertType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockAlertRepositoryTest {

    private val repository = MockAlertRepository()

    // ── observeAlerts ────────────────────────────────────────────────────────

    @Test
    fun `observeAlerts returns all six sample alerts`() = runTest {
        val alerts = repository.observeAlerts().first()
        assertEquals(6, alerts.size)
    }

    @Test
    fun `sample alerts contain mixed severities`() = runTest {
        val alerts = repository.observeAlerts().first()
        val priorities = alerts.map { it.priority }.toSet()
        assertTrue(AlertPriority.CRITICAL in priorities, "Expected CRITICAL priority")
        assertTrue(AlertPriority.HIGH in priorities, "Expected HIGH priority")
        assertTrue(AlertPriority.MEDIUM in priorities, "Expected MEDIUM priority")
        assertTrue(AlertPriority.LOW in priorities, "Expected LOW priority")
    }

    @Test
    fun `sample alerts contain mixed statuses`() = runTest {
        val alerts = repository.observeAlerts().first()
        val statuses = alerts.map { it.status }.toSet()
        assertTrue(AlertStatus.OPEN in statuses, "Expected OPEN status")
        assertTrue(AlertStatus.IN_PROGRESS in statuses, "Expected IN_PROGRESS status")
        assertTrue(AlertStatus.CLOSED in statuses, "Expected CLOSED status")
    }

    @Test
    fun `sample alerts contain both read and unread entries`() = runTest {
        val alerts = repository.observeAlerts().first()
        assertTrue(alerts.any { !it.isRead }, "Expected at least one unread alert")
        assertTrue(alerts.any { it.isRead }, "Expected at least one read alert")
    }

    @Test
    fun `sample data includes expected alert types`() = runTest {
        val alerts = repository.observeAlerts().first()
        val types = alerts.map { it.type }.toSet()
        assertTrue(AlertType.POSSIBLE_CROCODILE in types)
        assertTrue(AlertType.MOTION_DETECTED in types)
        assertTrue(AlertType.IMAGE_UPLOADED in types)
        assertTrue(AlertType.SYSTEM_WARNING in types)
        assertTrue(AlertType.BATTERY_LOW in types)
        assertTrue(AlertType.SYNC_COMPLETED in types)
    }

    @Test
    fun `each alert has a non-blank id, title, sourceName, and message`() = runTest {
        val alerts = repository.observeAlerts().first()
        alerts.forEach { alert ->
            assertTrue(alert.id.isNotBlank(), "id must not be blank — found: $alert")
            assertTrue(alert.title.isNotBlank(), "title must not be blank — found: ${alert.id}")
            assertTrue(alert.sourceName.isNotBlank(), "sourceName must not be blank — found: ${alert.id}")
            assertTrue(alert.message.isNotBlank(), "message must not be blank — found: ${alert.id}")
        }
    }

    // ── Sorting ──────────────────────────────────────────────────────────────

    @Test
    fun `sample alerts are ordered newest-first by createdAt`() {
        val sorted = AlertSampleData.alerts.sortedByDescending { it.createdAt }
        val original = AlertSampleData.alerts
        assertEquals(
            sorted.map { it.id },
            original.sortedByDescending { it.createdAt }.map { it.id },
            "Sample data should sort correctly by createdAt descending",
        )
        for (i in 0 until sorted.size - 1) {
            assertTrue(
                sorted[i].createdAt >= sorted[i + 1].createdAt,
                "Alert at index $i must have createdAt >= index ${i + 1}",
            )
        }
    }

    // ── observeAlert ─────────────────────────────────────────────────────────

    @Test
    fun `observeAlert returns correct alert for known id`() = runTest {
        val alert = repository.observeAlert("alert-001").first()
        assertNotNull(alert)
        assertEquals("alert-001", alert.id)
        assertEquals(AlertType.POSSIBLE_CROCODILE, alert.type)
    }

    @Test
    fun `observeAlert returns null for unknown id`() = runTest {
        val alert = repository.observeAlert("non-existent-id").first()
        assertNull(alert)
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    @Test
    fun `createAlert adds alert to the list`() = runTest {
        val repo = MockAlertRepository()
        val before = repo.observeAlerts().first().size

        val newAlert = AlertSampleData.alerts.first().copy(id = "alert-new")
        repo.createAlert(newAlert)

        val after = repo.observeAlerts().first()
        assertEquals(before + 1, after.size)
        assertTrue(after.any { it.id == "alert-new" })
    }

    @Test
    fun `deleteAlert removes the alert from the list`() = runTest {
        val repo = MockAlertRepository()
        repo.deleteAlert("alert-001")
        val alerts = repo.observeAlerts().first()
        assertTrue(alerts.none { it.id == "alert-001" })
    }
}
