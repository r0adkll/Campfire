package app.campfire.user.test

import app.campfire.core.model.User
import app.campfire.user.api.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeUserRepository : UserRepository {

  val currentUserFlow = MutableSharedFlow<User>(replay = 1)
  override fun observeCurrentUser(): Flow<User> {
    return currentUserFlow
  }

  var currentUser: User? = null
  override suspend fun getCurrentUser(): User {
    return currentUser!!
  }
}
