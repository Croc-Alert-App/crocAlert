package crocalert.app.model



data class UserContactPoint(
    val id: String = "",
    val userId: String = "",
    val type: ContactPointType = ContactPointType.EMAIL,
    val value: String = "",
    val label: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long? = null
)