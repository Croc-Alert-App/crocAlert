package crocalert.app.shared.sync

data class SyncPreferences(
    val alertsTtlMinutes: Int = 5,
    val camerasTtlMinutes: Int = 15,
    val sitesTtlMinutes: Int = 60,
    /** How many days back to count active alerts on the dashboard KPI. Default = 7. */
    val alertWindowDays: Int = 7,
)
