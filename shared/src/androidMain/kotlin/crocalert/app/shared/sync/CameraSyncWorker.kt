package crocalert.app.shared.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import crocalert.app.shared.AppModule

class CameraSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            AppModule.syncCameras()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
