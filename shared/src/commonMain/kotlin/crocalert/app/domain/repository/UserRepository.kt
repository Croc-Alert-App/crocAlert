package crocalert.app.domain.repository

import crocalert.app.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUsers(): Flow<List<User>>
    fun observeUser(userId: String): Flow<User?>

    suspend fun createUser(user: User): String
    suspend fun updateUser(user: User)
    suspend fun deleteUser(userId: String)
}