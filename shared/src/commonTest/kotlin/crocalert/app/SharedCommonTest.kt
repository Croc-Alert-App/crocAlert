package crocalert.app

import crocalert.app.model.AlertPriority
import crocalert.app.model.AlertStatus
import crocalert.app.shared.data.dto.AlertDto
import crocalert.app.shared.data.mapper.toDto
import crocalert.app.shared.data.mapper.toModel
import crocalert.app.shared.network.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AlertMapperTest {

    private val sampleDto = AlertDto(
        id = "abc",
        captureId = "cap-1",
        createdAt = 1000L,
        status = "IN_PROGRESS",
        priority = "HIGH",
        assignedToUserId = "user-42",
        closedAt = null,
        notes = "note",
        title = "Test alert"
    )

    @Test
    fun `toModel maps all fields correctly`() {
        val model = sampleDto.toModel()
        assertEquals("abc", model.id)
        assertEquals("cap-1", model.captureId)
        assertEquals(1000L, model.createdAt)
        assertEquals(AlertStatus.IN_PROGRESS, model.status)
        assertEquals(AlertPriority.HIGH, model.priority)
        assertEquals("user-42", model.assignedToUserId)
        assertNull(model.closedAt)
        assertEquals("note", model.notes)
        assertEquals("Test alert", model.title)
    }

    @Test
    fun `toModel defaults to OPEN for unknown status`() {
        val model = sampleDto.copy(status = "UNKNOWN_STATUS").toModel()
        assertEquals(AlertStatus.OPEN, model.status)
    }

    @Test
    fun `toModel defaults to MEDIUM for unknown priority`() {
        val model = sampleDto.copy(priority = "SUPER_CRITICAL").toModel()
        assertEquals(AlertPriority.MEDIUM, model.priority)
    }

    @Test
    fun `roundtrip toModel then toDto preserves all fields`() {
        val dto = sampleDto.toModel().toDto()
        assertEquals(sampleDto.id, dto.id)
        assertEquals(sampleDto.captureId, dto.captureId)
        assertEquals(sampleDto.createdAt, dto.createdAt)
        assertEquals(sampleDto.status, dto.status)
        assertEquals(sampleDto.priority, dto.priority)
        assertEquals(sampleDto.assignedToUserId, dto.assignedToUserId)
        assertEquals(sampleDto.closedAt, dto.closedAt)
        assertEquals(sampleDto.notes, dto.notes)
        assertEquals(sampleDto.title, dto.title)
    }
}

class ApiResultTest {

    @Test
    fun `Success holds data`() {
        val result: ApiResult<Int> = ApiResult.Success(42)
        assertIs<ApiResult.Success<Int>>(result)
        assertEquals(42, result.data)
    }

    @Test
    fun `Error holds message and optional http status code`() {
        val result = ApiResult.Error("Not found", code = 404)
        assertIs<ApiResult.Error>(result)
        assertEquals("Not found", result.message)
        assertEquals(404, result.code)
    }

    @Test
    fun `Error code defaults to null`() {
        val result = ApiResult.Error("Network error")
        assertNull(result.code)
    }
}
