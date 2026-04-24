package crocalert.app.shared

/**
 * In-memory singleton for the currently authenticated user.
 *
 * Populated by [FirebaseAuthClient] after a successful sign-in or TOTP verification.
 * Cleared by [SessionManager.forgetDevice] on logout.
 *
 * The [displayName] and [role] are encoded in the Firebase Auth profile as
 * `"FullName::RoleString"` (e.g. `"Juan Diego Sequeira::Administrador"`), set at
 * registration time via [FirebaseAuthClient.register].
 */
object UserSession {

    enum class UserRole { Admin, Guest }

    var displayName: String = ""
        private set

    var email: String = ""
        private set

    var role: UserRole = UserRole.Guest
        private set

    val isAdmin: Boolean get() = role == UserRole.Admin

    /** Full name as entered at registration (everything before `::` in displayName). */
    val fullName: String get() = displayName.substringBefore(SEPARATOR).trim()

    /** Human-readable role label (everything after `::` in displayName). */
    val roleLabel: String
        get() = when (role) {
            UserRole.Admin -> "Administrador"
            UserRole.Guest -> "Experto SINAC"
        }

    /**
     * Populates [displayName], [email], and [role] after a successful sign-in.
     *
     * Role resolution priority (first non-null wins):
     * 1. [roleOverride] — value from Firebase ID token custom claims (`role` claim).
     *    Set via Firebase Admin SDK / console: `{role: "Administrador"}`.
     * 2. Encoded in [rawDisplayName] as `"FullName::RoleString"` — set at registration.
     * 3. Falls back to [UserRole.Guest] when neither source has role data.
     *    This covers existing accounts created before role encoding was introduced.
     */
    fun populate(rawDisplayName: String?, email: String, roleOverride: String? = null) {
        this.email = email
        val name = rawDisplayName ?: email.substringBefore("@")
        val parts = name.split(SEPARATOR, limit = 2)
        displayName = parts[0].trim()
        val resolvedRole = roleOverride?.trim() ?: parts.getOrNull(1)?.trim()
        role = when (resolvedRole) {
            "Administrador" -> UserRole.Admin
            else -> UserRole.Guest
        }
    }

    /** Encodes a full name + role string for storage in Firebase displayName. */
    fun encode(fullName: String, role: String): String = "$fullName$SEPARATOR$role"

    /** Clears the session on logout. */
    fun clear() {
        displayName = ""
        email = ""
        role = UserRole.Guest
    }

    private const val SEPARATOR = "::"
}
