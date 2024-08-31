package app.campfire.account.api

import app.campfire.core.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

  /**
   * Observe the user for the current logged in server
   */
  fun observeCurrentUser(): Flow<User>
}
