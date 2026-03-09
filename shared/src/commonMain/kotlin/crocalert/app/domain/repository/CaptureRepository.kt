package crocalert.app.domain.repository
import crocalert.app.model.Capture
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCaptures(
        cameraId: String? = null,
        siteId: String? = null,
        limit: Int = 100
    ): Flow<List<Capture>>

    suspend fun createCapture(capture: Capture): String
    suspend fun deleteCapture(captureId: String)
}