package crocalert.app.shared.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import crocalert.app.shared.AppModule

class AlertSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            AppModule.syncAlerts()
            Result.success()
        } catch (e: Exception) {
            // After 3 attempts assume unrecoverable (auth failure, init error, etc.)
            // and surface as a permanent failure rather than retrying indefinitely.
            if (runAttemptCount >= 3) Result.failure() else Result.retry()
        }
    }
}
