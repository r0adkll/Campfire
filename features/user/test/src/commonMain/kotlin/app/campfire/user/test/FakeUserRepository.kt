// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.test

import app.campfire.core.model.User
import app.campfire.user.api.UserRepository
import app.campfire.user.test.fixtures.user
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserRepository : UserRepository {

  val currentStatefulUserFlow = MutableStateFlow(user("fake_user_id"))
  override val userFlow: StateFlow<User>
    get() = currentStatefulUserFlow.asStateFlow()

  val currentUserFlow = MutableSharedFlow<User>(replay = 1)
  override fun observeCurrentUser(): Flow<User> {
    return currentUserFlow
  }

  var currentUser: User? = null
  override suspend fun getCurrentUser(): User {
    return currentUser!!
  }
}
