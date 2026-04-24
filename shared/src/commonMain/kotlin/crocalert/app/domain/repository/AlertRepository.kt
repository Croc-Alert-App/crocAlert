package crocalert.app.domain.repository

import crocalert.app.model.Alert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AlertRepository {
    /** Returns a live stream of all cached alerts, updated whenever the cache refreshes. */
    fun observeAlerts(): Flow<List<Alert>>

    /** Returns a live stream for a single alert; emits null when the alert does not exist. */
    fun observeAlert(alertId: String): Flow<Alert?>

    /** Non-null when a post-mutation cache refresh fails; resets to null on next successful refresh. */
    val lastRefreshError: StateFlow<String?>

    /** Creates the alert on the server and returns the server-generated ID. */
    suspend fun createAlert(alert: Alert): String

    /** Updates the alert on the server; validates the status transition before the network call (R-01). */
    suspend fun updateAlert(alert: Alert)

    /** Deletes the alert by ID from the server and removes it from the local cache. */
    suspend fun deleteAlert(alertId: String)

    /** Forces a full re-sync regardless of TTL — called by the global refresh button. */
    suspend fun refresh()
}