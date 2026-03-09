package crocalert.app.model


data class Site(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long? = null,
    val centerLat: Double? = null,
    val centerLng: Double? = null,
    val region: String? = null
)