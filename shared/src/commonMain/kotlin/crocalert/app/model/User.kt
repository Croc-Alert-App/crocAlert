package crocalert.app.model



data class User(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val isActive: Boolean = true,
    val mfaEnabled: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long? = null,
    val lastLoginAt: Long? = null
)