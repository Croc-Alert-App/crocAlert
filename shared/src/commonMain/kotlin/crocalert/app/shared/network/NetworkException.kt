package crocalert.app.shared.network

/**
 * Thrown when an API call completes but the server returned an error response.
 *
 * @param message Human-readable error description from the server or a default.
 * @param httpStatus HTTP status code, if available (null for connectivity errors).
 */
class NetworkException(message: String, val httpStatus: Int? = null) : Exception(message)
