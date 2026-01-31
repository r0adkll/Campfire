package app.campfire.user.test

import app.campfire.core.model.User
import app.campfire.user.api.UserRepository
import app.campfire.user.test.fixtures.user
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeUserRepository : UserRepository {

  val currentUserFlow = MutableSharedFlow<User>(replay = 1)
  override fun observeCurrentUser(): Flow<User> {
    return currentUserFlow
  }

  val currentStatefulUserFlow = MutableStateFlow(user("fake_user_id"))
  override fun observeStatefulCurrentUser(): StateFlow<User> {
    return currentStatefulUserFlow
  }

  var currentUser: User? = null
  override suspend fun getCurrentUser(): User {
    return currentUser!!
  }
}
