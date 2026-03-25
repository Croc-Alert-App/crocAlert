package crocalert.app.shared.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WorkManagerSyncScheduler(private val context: Context) : SyncScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    override fun scheduleAlertSync(intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<AlertSyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            SyncWorkNames.ALERT_SYNC_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun scheduleCameraSync(intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<CameraSyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            SyncWorkNames.CAMERA_SYNC_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun scheduleSiteSync(intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<SiteSyncWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            SyncWorkNames.SITE_SYNC_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancelAll() {
        workManager.cancelAllWorkByTag(SyncWorkNames.ALERT_SYNC_PERIODIC)
        workManager.cancelAllWorkByTag(SyncWorkNames.CAMERA_SYNC_PERIODIC)
        workManager.cancelAllWorkByTag(SyncWorkNames.SITE_SYNC_PERIODIC)
    }
}
