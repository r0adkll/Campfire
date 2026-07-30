// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.api

import app.campfire.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {

  /**
   * Observe the current user as a [StateFlow].
   * Warning! If you observe this in a non-logged in composable
   * it will crash.
   */
  val userFlow: StateFlow<User>

  /**
   * Observe the user for the current logged in server
   */
  fun observeCurrentUser(): Flow<User>

  suspend fun getCurrentUser(): User
}
