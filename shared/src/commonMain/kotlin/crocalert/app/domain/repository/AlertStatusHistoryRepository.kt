package crocalert.app.domain.repository

import crocalert.app.model.AlertStatusHistory
import kotlinx.coroutines.flow.Flow

interface AlertStatusHistoryRepository {
    /** Returns a live stream of status history entries for [alertId], ordered by time. */
    fun observeHistory(alertId: String): Flow<List<AlertStatusHistory>>

    /** Appends a new history entry for a status transition and returns the server-generated ID. */
    suspend fun append(history: AlertStatusHistory): String
}