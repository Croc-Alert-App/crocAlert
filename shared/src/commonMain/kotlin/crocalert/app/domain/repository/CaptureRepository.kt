package crocalert.app.domain.repository
import crocalert.app.model.Capture
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    /**
     * Returns a live stream of captures, optionally filtered by [cameraId] or [siteId].
     * Results are capped at [limit] entries.
     */
    fun observeCaptures(
        cameraId: String? = null,
        siteId: String? = null,
        limit: Int = 100
    ): Flow<List<Capture>>

    /** Creates the capture record on the server and returns the server-generated ID. */
    suspend fun createCapture(capture: Capture): String

    /** Deletes the capture by ID from the server. */
    suspend fun deleteCapture(captureId: String)
}