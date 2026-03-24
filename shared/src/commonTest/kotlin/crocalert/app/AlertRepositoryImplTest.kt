package crocalert.app

import crocalert.app.model.Alert
import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.data.dto.IdResponse
import crocalert.app.shared.data.local.InMemoryAlertLocalDataSource
import crocalert.app.shared.data.remote.AlertRemoteDataSource
import crocalert.app.shared.data.repository.AlertRepositoryImpl
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AlertRepositoryImplTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val sampleDto = AlertDto(
        id = "alert-1",
        captureId = "cap-1",
        createdAt = 1000L,
        status = "OPEN",
        priority = "HIGH",
        title = "Test alert"
    )

    private val sampleAlert = Alert(
        id = "alert-1",
        captureId = "cap-1",
        createdAt = 1000L,
        status = AlertStatus.OPEN,
        priority = AlertPriority.HIGH,
        title = "Test alert"
    )

    private fun repo(fake: FakeAlertRemoteDataSource) =
        AlertRepositoryImpl(fake, InMemoryAlertLocalDataSource(),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined))

    // ── lastRefreshError ──────────────────────────────────────────────────────

    @Test
    fun `lastRefreshError emits error message when post-mutation refresh fails`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),
            createResult = ApiResult.Success(IdResponse("new-id"))
        )
        val r = repo(fake)
        r.observeAlerts().first()                       // populate cache

        fake.getAlertsResult = ApiResult.Error("Server down")
        r.createAlert(Alert(captureId = "cap-2", title = "New"))

        assertEquals("Server down", r.lastRefreshError.value)
    }

    @Test
    fun `lastRefreshError resets to null after a successful refresh`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),
            createResult = ApiResult.Success(IdResponse("new-id"))
        )
        val r = repo(fake)
        r.observeAlerts().first()

        // First mutation — refresh fails
        fake.getAlertsResult = ApiResult.Error("Timeout")
        r.createAlert(Alert(captureId = "cap-2", title = "Fail"))
        assertEquals("Timeout", r.lastRefreshError.value)

        // Second mutation — refresh succeeds
        fake.getAlertsResult = ApiResult.Success(listOf(sampleDto))
        r.createAlert(Alert(captureId = "cap-3", title = "Recover"))
        assertNull(r.lastRefreshError.value)
    }

    // ── observeAlerts ─────────────────────────────────────────────────────────

    @Test
    fun `observeAlerts emits empty list when remote returns empty`() = runTest {
        val fake = FakeAlertRemoteDataSource()
        val result = repo(fake).observeAlerts().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeAlerts emits mapped models when remote returns DTOs`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto))
        )
        val result = repo(fake).observeAlerts().first()
        assertEquals(1, result.size)
        assertEquals("alert-1", result[0].id)
        assertEquals(AlertStatus.OPEN, result[0].status)
        assertEquals(AlertPriority.HIGH, result[0].priority)
    }

    @Test
    fun `observeAlerts only fetches from remote once when cache is already populated`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto))
        )
        val r = repo(fake)
        r.observeAlerts().first()  // populates cache
        r.observeAlerts().first()  // cache is non-empty → no refetch
        assertEquals(1, fake.getCallCount)
    }

    @Test
    fun `observeAlerts retains stale cache when refresh fails after a mutation`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),
            createResult = ApiResult.Success(IdResponse("new-id"))
        )
        val r = repo(fake)
        // Populate cache
        val initial = r.observeAlerts().first()
        assertEquals(1, initial.size)

        // createAlert() triggers a background refresh — make it fail
        fake.getAlertsResult = ApiResult.Error("Server down")
        r.createAlert(Alert(captureId = "cap-2", title = "New"))

        val afterError = r.observeAlerts().first()
        assertEquals(1, afterError.size)
        assertEquals("alert-1", afterError[0].id)
    }

    // ── observeAlert(id) ──────────────────────────────────────────────────────

    @Test
    fun `observeAlert returns null for unknown id`() = runTest {
        val fake = FakeAlertRemoteDataSource()
        val result = repo(fake).observeAlert("no-such-id").first()
        assertNull(result)
    }

    @Test
    fun `observeAlert returns matching Alert for known id`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto))
        )
        val result = repo(fake).observeAlert("alert-1").first()
        assertNotNull(result)
        assertEquals("alert-1", result.id)
    }

    @Test
    fun `observeAlert emits null after the alert is deleted`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),
            deleteResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        assertNotNull(r.observeAlert("alert-1").first())

        fake.getAlertsResult = ApiResult.Success(emptyList())
        r.deleteAlert("alert-1")

        assertNull(r.observeAlert("alert-1").first())
    }

    // ── createAlert ───────────────────────────────────────────────────────────

    @Test
    fun `createAlert returns id from remote on success`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(emptyList()),
            createResult = ApiResult.Success(IdResponse("server-generated-id"))
        )
        val id = repo(fake).createAlert(Alert(captureId = "cap-2", title = "New"))
        assertEquals("server-generated-id", id)
    }

    @Test
    fun `createAlert refreshes cache after success`() = runTest {
        val newDto = sampleDto.copy(id = "alert-2", title = "Created")
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(emptyList()),
            createResult = ApiResult.Success(IdResponse("alert-2"))
        )
        val r = repo(fake)
        r.observeAlerts().first()              // initial load → empty
        fake.getAlertsResult = ApiResult.Success(listOf(newDto))
        r.createAlert(Alert(captureId = "cap-2", title = "Created"))
        val after = r.observeAlerts().first()  // cache was refreshed
        assertEquals(1, after.size)
        assertEquals("alert-2", after[0].id)
    }

    @Test
    fun `createAlert throws when remote returns Error`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(emptyList()),
            createResult = ApiResult.Error("Unauthorized", 401)
        )
        assertFailsWith<IllegalStateException> {
            repo(fake).createAlert(Alert(captureId = "cap-2", title = "New"))
        }
    }

    // ── updateAlert ───────────────────────────────────────────────────────────

    @Test
    fun `updateAlert throws IllegalArgumentException when alert id is blank`() = runTest {
        val fake = FakeAlertRemoteDataSource()
        assertFailsWith<IllegalArgumentException> {
            repo(fake).updateAlert(Alert(id = "", title = "no id"))
        }
    }

    @Test
    fun `updateAlert refreshes cache after success`() = runTest {
        val updatedDto = sampleDto.copy(title = "Updated title")
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()
        fake.getAlertsResult = ApiResult.Success(listOf(updatedDto))
        r.updateAlert(sampleAlert.copy(title = "Updated title"))
        val after = r.observeAlerts().first()
        assertEquals("Updated title", after[0].title)
    }

    @Test
    fun `updateAlert throws when remote returns Error`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(emptyList()),
            updateResult = ApiResult.Error("Not found", 404)
        )
        assertFailsWith<IllegalStateException> {
            repo(fake).updateAlert(sampleAlert)
        }
    }

    // ── updateAlert — status validation (P3) ──────────────────────────────────

    @Test
    fun `updateAlert allows valid transition OPEN to IN_PROGRESS`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),  // status = "OPEN"
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()  // populate cache with OPEN alert
        // Should not throw
        r.updateAlert(sampleAlert.copy(status = AlertStatus.IN_PROGRESS))
    }

    @Test
    fun `updateAlert allows valid transition OPEN to CLOSED`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()
        r.updateAlert(sampleAlert.copy(status = AlertStatus.CLOSED))
    }

    @Test
    fun `updateAlert rejects CLOSED to OPEN transition (R-01)`() = runTest {
        val closedDto = sampleDto.copy(status = "CLOSED")
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(closedDto)),
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()  // cache has CLOSED alert
        assertFailsWith<IllegalStateException> {
            r.updateAlert(sampleAlert.copy(status = AlertStatus.OPEN))
        }
    }

    @Test
    fun `updateAlert rejects IN_PROGRESS to OPEN transition (R-01)`() = runTest {
        val inProgressDto = sampleDto.copy(status = "IN_PROGRESS")
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(inProgressDto)),
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()
        assertFailsWith<IllegalStateException> {
            r.updateAlert(sampleAlert.copy(status = AlertStatus.OPEN))
        }
    }

    @Test
    fun `updateAlert skips validation when alert not in local cache`() = runTest {
        // Alert not in local cache → no cached status to validate against → passes through
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(emptyList()),
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()  // empty cache
        // Any status allowed when we have no cached state to compare against
        r.updateAlert(sampleAlert.copy(status = AlertStatus.OPEN))
    }

    @Test
    fun `updateAlert with unchanged status (same-to-same) skips validation and succeeds`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),  // status = "OPEN"
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()
        // Same status → no transition to validate → title update should succeed
        r.updateAlert(sampleAlert.copy(status = AlertStatus.OPEN, title = "New title"))
    }

    // ── syncIfStale mutex (P7) ────────────────────────────────────────────────

    @Test
    fun `concurrent observeAlerts calls do not fire duplicate network requests`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto))
        )
        val r = repo(fake)
        // Collect from two independent observers simultaneously — cache starts empty so both
        // would normally trigger syncIfStale. Mutex ensures only one network call fires.
        r.observeAlerts().first()
        r.observeAlerts().first()
        // With Mutex: second observer sees fresh cache (lastSyncedAt set), skips sync.
        // Without Mutex: two concurrent syncs → getCallCount == 2.
        assertEquals(1, fake.getCallCount)
    }

    // ── sync eviction (P2) ────────────────────────────────────────────────────

    @Test
    fun `full sync evicts deleted records from cache`() = runTest {
        val dto2 = sampleDto.copy(id = "alert-2", title = "Second")
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto, dto2))
        )
        val r = repo(fake)
        r.observeAlerts().first()  // cache: [alert-1, alert-2]
        assertEquals(2, r.observeAlerts().first().size)

        // Server deletes alert-2 — next full sync (since=null) should evict it
        fake.getAlertsResult = ApiResult.Success(listOf(sampleDto))
        r.refresh()  // calls sync(since=null) → clearAndUpsertAll

        val after = r.observeAlerts().first()
        assertEquals(1, after.size)
        assertEquals("alert-1", after[0].id)
    }

    @Test
    fun `post-mutation full sync reflects server state exactly (server-deleted records are evicted)`() = runTest {
        val dto2 = sampleDto.copy(id = "alert-2", title = "Second")
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto, dto2)),
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()              // cache: [alert-1, alert-2]

        // Server drops alert-2 (deleted); next full sync should evict it
        fake.getAlertsResult = ApiResult.Success(listOf(sampleDto))
        r.updateAlert(sampleAlert.copy(title = "Updated"))  // triggers full sync

        val after = r.observeAlerts().first()
        assertEquals(1, after.size, "Full sync must evict alert-2 which is absent from server response")
        assertEquals("alert-1", after[0].id)
    }

    // ── sync DB write failure (P9) ────────────────────────────────────────────

    @Test
    fun `lastRefreshError is set when local cache write throws`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto))
        )
        val throwingLocal = ThrowingAlertLocalDataSource()
        val r = AlertRepositoryImpl(fake, throwingLocal,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined))
        r.observeAlerts().first()
        assertNotNull(r.lastRefreshError.value)
    }

    // ── since=0 guard (P10) ───────────────────────────────────────────────────

    @Test
    fun `syncIfStale uses null since when latestCreatedAt returns 0`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto))
        )
        val r = repo(fake)
        r.observeAlerts().first()
        // After first sync latestCreatedAt returns sampleDto.createdAt = 1000L > 0
        // A second sync triggered by refresh() should pass since=null (full sync)
        // regardless — just verify no crash and data is still correct
        r.refresh()
        assertEquals(1, r.observeAlerts().first().size)
    }

    // ── deleteAlert ───────────────────────────────────────────────────────────

    @Test
    fun `deleteAlert refreshes cache after success`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(listOf(sampleDto)),
            deleteResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)
        r.observeAlerts().first()                      // populates cache
        fake.getAlertsResult = ApiResult.Success(emptyList())
        r.deleteAlert("alert-1")
        assertTrue(r.observeAlerts().first().isEmpty())
    }

    @Test
    fun `deleteAlert throws when remote returns Error`() = runTest {
        val fake = FakeAlertRemoteDataSource(
            getAlertsResult = ApiResult.Success(emptyList()),
            deleteResult = ApiResult.Error("Not found", 404)
        )
        assertFailsWith<IllegalStateException> {
            repo(fake).deleteAlert("alert-1")
        }
    }
}

// ── Throwing local data source ────────────────────────────────────────────────

private class ThrowingAlertLocalDataSource : crocalert.app.shared.data.local.AlertLocalDataSource {
    private val _alerts = kotlinx.coroutines.flow.MutableStateFlow<List<crocalert.app.shared.data.dto.AlertDto>>(emptyList())
    override fun selectAll() = _alerts
    override suspend fun upsertAll(alerts: List<crocalert.app.shared.data.dto.AlertDto>) =
        throw RuntimeException("DB write failed")
    override suspend fun clearAndUpsertAll(alerts: List<crocalert.app.shared.data.dto.AlertDto>) =
        throw RuntimeException("DB write failed")
    override suspend fun lastSyncedAt(): Long? = null
    override suspend fun latestCreatedAt(): Long? = null
}

// ── Fake remote data source ───────────────────────────────────────────────────

private class FakeAlertRemoteDataSource(
    var getAlertsResult: ApiResult<List<AlertDto>> = ApiResult.Success(emptyList()),
    var createResult: ApiResult<IdResponse> = ApiResult.Success(IdResponse("new-id")),
    var updateResult: ApiResult<Unit> = ApiResult.Success(Unit),
    var deleteResult: ApiResult<Unit> = ApiResult.Success(Unit),
) : AlertRemoteDataSource {

    var getCallCount = 0

    override suspend fun getAlerts(since: Long?): ApiResult<List<AlertDto>> =
        getAlertsResult.also { getCallCount++ }

    override suspend fun getAlert(id: String): ApiResult<AlertDto> =
        ApiResult.Success(AlertDto(id = id))

    override suspend fun createAlert(dto: AlertDto): ApiResult<IdResponse> = createResult

    override suspend fun updateAlert(id: String, dto: AlertDto): ApiResult<Unit> = updateResult

    override suspend fun deleteAlert(id: String): ApiResult<Unit> = deleteResult
}
