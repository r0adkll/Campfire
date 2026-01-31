package app.campfire.user.api

import app.campfire.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {

  /**
   * Observe the user for the current logged in server
   */
  fun observeCurrentUser(): Flow<User>

  /**
   * Observe the current user as a [StateFlow].
   * Warning! If you observe this in a non-logged in composable
   * it will crash.
   */
  fun observeStatefulCurrentUser(): StateFlow<User>

  suspend fun getCurrentUser(): User
}
