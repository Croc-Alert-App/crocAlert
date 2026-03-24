package crocalert.app

import app.cash.turbine.test
import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.data.local.InMemoryAlertLocalDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests the AlertLocalDataSource contract using the in-memory implementation.
 * This validates behaviour expected from any AlertLocalDataSource (including SQLDelight).
 * The SqlDelight-backed impl is exercised on Android; JVM tests use InMemoryAlertLocalDataSource.
 */
class AlertLocalDataSourceTest {

    private fun makeDataSource() = InMemoryAlertLocalDataSource()

    private val alert1 = AlertDto(
        id        = "a-1",
        captureId = "cap-1",
        cameraId  = "cam-1",
        createdAt = 1000L,
        status    = "OPEN",
        priority  = "HIGH",
        title     = "Alert One",
    )
    private val alert2 = AlertDto(
        id        = "a-2",
        captureId = "cap-2",
        cameraId  = "cam-2",
        createdAt = 2000L,
        status    = "CLOSED",
        priority  = "MEDIUM",
        title     = "Alert Two",
    )

    // ── selectAll + upsertAll ────────────────────────────────────────────────

    @Test
    fun `selectAll emits empty list on fresh data source`() = runTest {
        val ds = makeDataSource()
        ds.selectAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsertAll stores alerts and selectAll emits them`() = runTest {
        val ds = makeDataSource()
        ds.upsertAll(listOf(alert1, alert2))
        ds.selectAll().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertTrue(items.any { it.id == "a-1" })
            assertTrue(items.any { it.id == "a-2" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsertAll replaces existing alert with same id (upsert semantics)`() = runTest {
        val ds = makeDataSource()
        ds.upsertAll(listOf(alert1))
        val updated = alert1.copy(title = "Updated Title")
        ds.upsertAll(listOf(updated))
        ds.selectAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Updated Title", items[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsertAll with empty list preserves existing cached alerts (merge semantics)`() = runTest {
        val ds = makeDataSource()
        ds.upsertAll(listOf(alert1, alert2))
        ds.upsertAll(emptyList())  // nothing to merge — existing records untouched
        ds.selectAll().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsertAll preserves older records not present in incremental update`() = runTest {
        val ds = makeDataSource()
        ds.upsertAll(listOf(alert1))           // seed: alert1
        ds.upsertAll(listOf(alert2))            // incremental: adds alert2, keeps alert1
        ds.selectAll().test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertTrue(items.any { it.id == "a-1" }, "alert1 must be preserved")
            assertTrue(items.any { it.id == "a-2" }, "alert2 must be added")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── clearAndUpsertAll ────────────────────────────────────────────────────

    @Test
    fun `clearAndUpsertAll replaces entire cache evicting absent records`() = runTest {
        val ds = makeDataSource()
        ds.upsertAll(listOf(alert1, alert2))   // seed both
        ds.clearAndUpsertAll(listOf(alert1))   // full sync: only alert1 returned
        ds.selectAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("a-1", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAndUpsertAll with empty list clears entire cache`() = runTest {
        val ds = makeDataSource()
        ds.upsertAll(listOf(alert1, alert2))
        ds.clearAndUpsertAll(emptyList())
        ds.selectAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAndUpsertAll updates lastSyncedAt`() = runTest {
        val ds = makeDataSource()
        val before = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        ds.clearAndUpsertAll(listOf(alert1))
        val after = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val syncedAt = ds.lastSyncedAt()
        assertNotNull(syncedAt)
        assertTrue(syncedAt in before..after)
    }

    // ── lastSyncedAt ─────────────────────────────────────────────────────────

    @Test
    fun `lastSyncedAt returns null before first upsert`() = runTest {
        val ds = makeDataSource()
        assertNull(ds.lastSyncedAt())
    }

    @Test
    fun `lastSyncedAt returns a recent timestamp after upsert`() = runTest {
        val ds = makeDataSource()
        val before = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        ds.upsertAll(listOf(alert1))
        val after = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val syncedAt = ds.lastSyncedAt()
        assertNotNull(syncedAt)
        assertTrue(syncedAt in before..after)
    }

    @Test
    fun `lastSyncedAt advances after each upsert`() = runTest {
        val ds = makeDataSource()
        ds.upsertAll(listOf(alert1))
        val first = ds.lastSyncedAt()!!
        // Small sleep not available in common — just upsert again and check >= first
        ds.upsertAll(listOf(alert2))
        val second = ds.lastSyncedAt()!!
        assertTrue(second >= first)
    }

    // ── reactivity ───────────────────────────────────────────────────────────

    @Test
    fun `selectAll emits again when upsertAll is called on an active collector`() = runTest {
        val ds = makeDataSource()
        ds.selectAll().test {
            assertTrue(awaitItem().isEmpty())             // initial emission
            ds.upsertAll(listOf(alert1))
            val second = awaitItem()
            assertEquals(1, second.size)
            assertEquals("a-1", second[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
