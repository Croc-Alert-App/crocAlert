package crocalert.app

import crocalert.app.model.Alert
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

/**
 * Supplementary cache-invalidation tests for [AlertRepositoryImpl].
 *
 * Focuses on scenarios not covered by [AlertRepositoryImplTest]:
 * - Sequential mutations each trigger a fresh remote fetch.
 * - Cache content is correct after each mutation step.
 * - Remote call count tracks invalidation correctly.
 */
@IntegrationTest
class AlertRepositoryCacheTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val dto1 = AlertDto(id = "a1", title = "Alert 1")
    private val dto2 = AlertDto(id = "a2", title = "Alert 2")
    private val dto3 = AlertDto(id = "a3", title = "Alert 3")

    private fun repo(fake: SequentialFakeRemote) =
        AlertRepositoryImpl(fake, InMemoryAlertLocalDataSource(),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined))

    // ── Sequential create invalidation ────────────────────────────────────────

    @Test
    fun `each createAlert triggers exactly one additional remote fetch`() = runTest {
        val fake = SequentialFakeRemote(
            getSequence = listOf(
                ApiResult.Success(listOf(dto1)),           // initial load
                ApiResult.Success(listOf(dto1, dto2)),     // after first create
                ApiResult.Success(listOf(dto1, dto2, dto3)) // after second create
            )
        )
        val r = repo(fake)

        r.observeAlerts().first()                                      // fetch #1 (initial)
        assertEquals(1, fake.getCallCount)

        r.createAlert(Alert(captureId = "c2", title = "Alert 2"))      // fetch #2
        assertEquals(2, fake.getCallCount)

        r.createAlert(Alert(captureId = "c3", title = "Alert 3"))      // fetch #3
        assertEquals(3, fake.getCallCount)
    }

    @Test
    fun `cache reflects updated list after two sequential creates`() = runTest {
        val fake = SequentialFakeRemote(
            getSequence = listOf(
                ApiResult.Success(emptyList()),
                ApiResult.Success(listOf(dto1)),
                ApiResult.Success(listOf(dto1, dto2))
            ),
            createResult = ApiResult.Success(IdResponse("generated"))
        )
        val r = repo(fake)

        r.observeAlerts().first()                                      // empty

        r.createAlert(Alert(captureId = "c1", title = "Alert 1"))
        val afterFirst = r.observeAlerts().first()
        assertEquals(1, afterFirst.size)
        assertEquals("a1", afterFirst[0].id)

        r.createAlert(Alert(captureId = "c2", title = "Alert 2"))
        val afterSecond = r.observeAlerts().first()
        assertEquals(2, afterSecond.size)
    }

    // ── Update then observe ───────────────────────────────────────────────────

    @Test
    fun `cache reflects updated title after updateAlert`() = runTest {
        val updatedDto = dto1.copy(title = "Updated title")
        val fake = SequentialFakeRemote(
            getSequence = listOf(
                ApiResult.Success(listOf(dto1)),         // initial
                ApiResult.Success(listOf(updatedDto))    // after update
            ),
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)

        r.observeAlerts().first()                        // populate cache
        r.updateAlert(Alert(id = "a1", title = "Updated title"))

        val after = r.observeAlerts().first()
        assertEquals(1, after.size)
        assertEquals("Updated title", after[0].title)
    }

    @Test
    fun `updateAlert triggers exactly one remote fetch beyond initial load`() = runTest {
        val fake = SequentialFakeRemote(
            getSequence = listOf(
                ApiResult.Success(listOf(dto1)),
                ApiResult.Success(listOf(dto1.copy(title = "Updated")))
            ),
            updateResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)

        r.observeAlerts().first()
        assertEquals(1, fake.getCallCount)

        r.updateAlert(Alert(id = "a1", title = "Updated"))
        assertEquals(2, fake.getCallCount)
    }

    // ── Delete then observe ───────────────────────────────────────────────────

    @Test
    fun `deleteAlert reduces cache size by one`() = runTest {
        val fake = SequentialFakeRemote(
            getSequence = listOf(
                ApiResult.Success(listOf(dto1, dto2)),   // initial
                ApiResult.Success(listOf(dto2))          // after deleting a1
            ),
            deleteResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)

        val initial = r.observeAlerts().first()
        assertEquals(2, initial.size)

        r.deleteAlert("a1")
        val after = r.observeAlerts().first()
        assertEquals(1, after.size)
        assertEquals("a2", after[0].id)
    }

    @Test
    fun `deleteAlert triggers exactly one remote fetch beyond initial load`() = runTest {
        val fake = SequentialFakeRemote(
            getSequence = listOf(
                ApiResult.Success(listOf(dto1)),
                ApiResult.Success(emptyList())
            ),
            deleteResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)

        r.observeAlerts().first()
        assertEquals(1, fake.getCallCount)

        r.deleteAlert("a1")
        assertEquals(2, fake.getCallCount)
    }

    // ── Mixed mutation sequence ───────────────────────────────────────────────

    @Test
    fun `create then delete leaves cache empty`() = runTest {
        val fake = SequentialFakeRemote(
            getSequence = listOf(
                ApiResult.Success(emptyList()),          // initial
                ApiResult.Success(listOf(dto1)),         // after create
                ApiResult.Success(emptyList())           // after delete
            ),
            createResult = ApiResult.Success(IdResponse("a1")),
            deleteResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)

        r.observeAlerts().first()                        // empty

        r.createAlert(Alert(captureId = "c1", title = "A"))
        val afterCreate = r.observeAlerts().first()
        assertEquals(1, afterCreate.size)

        r.deleteAlert("a1")
        val afterDelete = r.observeAlerts().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `total remote call count after create plus delete is initial plus two`() = runTest {
        val fake = SequentialFakeRemote(
            getSequence = listOf(
                ApiResult.Success(emptyList()),
                ApiResult.Success(listOf(dto1)),
                ApiResult.Success(emptyList())
            ),
            createResult = ApiResult.Success(IdResponse("a1")),
            deleteResult = ApiResult.Success(Unit)
        )
        val r = repo(fake)

        r.observeAlerts().first()                           // call #1
        r.createAlert(Alert(captureId = "c1", title = "A")) // call #2
        r.deleteAlert("a1")                                 // call #3

        assertEquals(3, fake.getCallCount)
    }
}

// ── Sequential fake with configurable response list ───────────────────────────

private class SequentialFakeRemote(
    private val getSequence: List<ApiResult<List<AlertDto>>> = listOf(ApiResult.Success(emptyList())),
    private val createResult: ApiResult<IdResponse> = ApiResult.Success(IdResponse("new-id")),
    private val updateResult: ApiResult<Unit> = ApiResult.Success(Unit),
    private val deleteResult: ApiResult<Unit> = ApiResult.Success(Unit),
) : AlertRemoteDataSource {

    var getCallCount = 0
        private set

    override suspend fun getAlerts(): ApiResult<List<AlertDto>> {
        val result = getSequence.getOrElse(getCallCount) { getSequence.last() }
        getCallCount++
        return result
    }

    override suspend fun getAlert(id: String): ApiResult<AlertDto> =
        ApiResult.Success(AlertDto(id = id))

    override suspend fun createAlert(dto: AlertDto): ApiResult<IdResponse> = createResult

    override suspend fun updateAlert(id: String, dto: AlertDto): ApiResult<Unit> = updateResult

    override suspend fun deleteAlert(id: String): ApiResult<Unit> = deleteResult
}
