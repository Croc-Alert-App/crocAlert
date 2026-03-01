package crocalert.app.model



data class Capture(
    val id: String = "",
    val cameraId: String = "",
    val capturedAt: Long = 0L,
    val imageUrl: String = "",
    val thumbUrl: String? = null,
    val sha256: String? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val fileSizeBytes: Long? = null,
    val createdAt: Long = 0L,
    val source: String? = null,
    val externalFileId: String? = null,
    val externalCreatedAt: Long? = null,
    val externalModifiedAt: Long? = null,
    val metadataJson: String? = null
)