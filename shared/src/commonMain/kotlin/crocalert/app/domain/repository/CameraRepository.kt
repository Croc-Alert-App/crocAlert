package crocalert.app.domain.repository

import crocalert.app.model.Camera
import crocalert.app.shared.data.dto.CameraDailyStatsDto
import crocalert.app.shared.data.dto.CameraMonitoringDashboardDto
import crocalert.app.shared.data.dto.CaptureDto
import crocalert.app.shared.data.dto.GlobalDailyCaptureRateDto
import crocalert.app.shared.network.ApiResult
import kotlinx.coroutines.flow.Flow

interface CameraRepository {
    /** Returns a live stream of cameras; pass [siteId] to filter by site, or null for all cameras. */
    fun observeCameras(siteId: String? = null): Flow<List<Camera>>

    /** Returns a live stream for a single camera; emits null when the camera does not exist. */
    fun observeCamera(cameraId: String): Flow<Camera?>

    /** Fetches all capture records for [cameraId] from the server. */
    suspend fun getCapturesByCamera(cameraId: String): ApiResult<List<CaptureDto>>

    /** Fetches server-authoritative daily stats for [cameraId] on [date] (format: YYYY-MM-DD). */
    suspend fun getDailyStats(cameraId: String, date: String): ApiResult<CameraDailyStatsDto>

    /** Fetches daily stats for every camera on [date] (format: YYYY-MM-DD) in a single request. */
    suspend fun getDailyStatsForAll(date: String): ApiResult<List<CameraDailyStatsDto>>

    /** Fetches the full monitoring dashboard summary for [date] (format: YYYY-MM-DD). */
    suspend fun getMonitoringDashboard(date: String): ApiResult<CameraMonitoringDashboardDto>

    /** Fetches the global capture rate aggregated across all cameras for [date] (format: YYYY-MM-DD). */
    suspend fun getGlobalCaptureRate(date: String): ApiResult<GlobalDailyCaptureRateDto>

    /** Creates the camera on the server and returns the server-generated ID. */
    suspend fun createCamera(camera: Camera): String

    /** Updates camera fields on the server; also used to persist expectedImages changes from the history screen. */
    suspend fun updateCamera(camera: Camera)

    /** Deletes the camera by ID from the server and removes it from the local cache. */
    suspend fun deleteCamera(cameraId: String)

    /** Forces a full re-sync regardless of TTL. */
    suspend fun refresh()
}
