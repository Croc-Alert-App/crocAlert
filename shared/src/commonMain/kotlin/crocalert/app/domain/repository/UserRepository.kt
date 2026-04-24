package crocalert.app.domain.repository

import crocalert.app.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Returns a live stream of all cached users. */
    fun observeUsers(): Flow<List<User>>

    /** Returns a live stream for a single user; emits null when the user does not exist. */
    fun observeUser(userId: String): Flow<User?>

    /** Creates the user on the server and returns the server-generated ID. */
    suspend fun createUser(user: User): String

    /** Updates user fields on the server. */
    suspend fun updateUser(user: User)

    /** Deletes the user by ID from the server. */
    suspend fun deleteUser(userId: String)
}