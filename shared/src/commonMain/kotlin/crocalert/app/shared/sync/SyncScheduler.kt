package crocalert.app.shared.sync

interface SyncScheduler {
    fun scheduleAlertSync(intervalMinutes: Int)
    fun scheduleCameraSync(intervalMinutes: Int)
    fun scheduleSiteSync(intervalMinutes: Int)
    fun cancelAll()
}
