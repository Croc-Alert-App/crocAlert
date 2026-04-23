package crocalert.app.shared.sync

data class SyncPreferences(
    val alertsTtlMinutes: Int = 5,
    val camerasTtlMinutes: Int = 15,
    val sitesTtlMinutes: Int = 60,
)
