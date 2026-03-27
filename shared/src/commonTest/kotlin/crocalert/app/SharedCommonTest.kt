package crocalert.app

import crocalert.app.shared.network.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

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
