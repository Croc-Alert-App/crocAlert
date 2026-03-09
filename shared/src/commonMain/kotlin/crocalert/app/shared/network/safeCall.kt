package crocalert.app.shared.network

import io.ktor.client.plugins.*
import io.ktor.utils.io.errors.IOException

suspend inline fun <T> safeCall(block: () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: IOException) {
        ApiResult.Error("Network error")
    } catch (e: ClientRequestException) {
        ApiResult.Error("Client error")
    } catch (e: ServerResponseException) {
        ApiResult.Error("Server error")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unknown error")
    }
}