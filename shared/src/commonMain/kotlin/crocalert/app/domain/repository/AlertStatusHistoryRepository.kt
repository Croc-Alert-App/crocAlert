package crocalert.app.domain.repository

import crocalert.app.model.AlertStatusHistory
import kotlinx.coroutines.flow.Flow

interface AlertStatusHistoryRepository {
    fun observeHistory(alertId: String): Flow<List<AlertStatusHistory>>
    suspend fun append(history: AlertStatusHistory): String
}