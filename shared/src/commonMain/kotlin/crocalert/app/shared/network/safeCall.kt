package crocalert.app.shared.network

import io.ktor.client.plugins.*
import io.ktor.utils.io.errors.IOException

suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: IOException) {
        ApiResult.Error("Network error: ${e.message}")
    } catch (e: ClientRequestException) {
        val status = e.response.status.value
        ApiResult.Error("Client error ${status}: ${e.response.status.description}", code = status)
    } catch (e: ServerResponseException) {
        val status = e.response.status.value
        ApiResult.Error("Server error ${status}: ${e.response.status.description}", code = status)
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unknown error")
    }
}