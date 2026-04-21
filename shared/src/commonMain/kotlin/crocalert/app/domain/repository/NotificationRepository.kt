package crocalert.app.domain.repository

import crocalert.app.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /**
     * Returns a live stream of notifications, optionally filtered by [alertId] or [userId].
     * Results are capped at [limit] entries.
     */
    fun observeNotifications(
        alertId: String? = null,
        userId: String? = null,
        limit: Int = 100
    ): Flow<List<Notification>>

    /** Returns a live stream for a single notification; emits null when it does not exist. */
    fun observeNotification(notificationId: String): Flow<Notification?>

    /** Creates the notification on the server and returns the server-generated ID. */
    suspend fun createNotification(notification: Notification): String

    /** Updates notification fields on the server. */
    suspend fun updateNotification(notification: Notification)

    /** Deletes the notification by ID from the server. */
    suspend fun deleteNotification(notificationId: String)
}