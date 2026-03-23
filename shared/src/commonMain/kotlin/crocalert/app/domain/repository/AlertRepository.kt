package crocalert.app.domain.repository

import crocalert.app.model.Alert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun observeAlerts(): Flow<List<Alert>>
    fun observeAlert(alertId: String): Flow<Alert?>

    /**
     * Emits a non-null error message when a post-mutation refresh fails (i.e. the write
     * succeeded on the server but the subsequent GET could not update the local cache).
     * Resets to null after the next successful refresh.
     */
    val lastRefreshError: Flow<String?>

    suspend fun createAlert(alert: Alert): String
    suspend fun updateAlert(alert: Alert)
    suspend fun deleteAlert(alertId: String)

    /** Forces a full re-sync regardless of TTL — called by the global refresh button. */
    suspend fun refresh()
}