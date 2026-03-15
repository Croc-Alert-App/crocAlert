package crocalert.app.domain.repository
import kotlinx.coroutines.flow.Flow
import crocalert.app.model.Alert


interface AlertRepository {
    fun observeAlerts(): Flow<List<Alert>>
    fun observeAlert(alertId: String): Flow<Alert?>

    suspend fun createAlert(alert: Alert): String
    suspend fun updateAlert(alert: Alert)
    suspend fun deleteAlert(alertId: String)
}