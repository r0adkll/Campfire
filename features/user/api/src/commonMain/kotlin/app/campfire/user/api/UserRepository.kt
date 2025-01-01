package app.campfire.user.api

import app.campfire.core.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

  /**
   * Observe the user for the current logged in server
   */
  fun observeCurrentUser(): Flow<User>

  suspend fun getCurrentUser(): User
}
