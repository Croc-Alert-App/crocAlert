package crocalert.app.domain.repository

import crocalert.app.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(
        alertId: String? = null,
        userId: String? = null,
        limit: Int = 100
    ): Flow<List<Notification>>

    fun observeNotification(notificationId: String): Flow<Notification?>

    suspend fun createNotification(notification: Notification): String
    suspend fun updateNotification(notification: Notification)
    suspend fun deleteNotification(notificationId: String)
}