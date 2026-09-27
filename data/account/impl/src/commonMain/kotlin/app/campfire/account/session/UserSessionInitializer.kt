// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.session

import app.campfire.account.api.UserSessionManager
import app.campfire.account.api.di.UserGraphManager
import app.campfire.core.app.UserInitializer
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class UserSessionInitializer(
  private val userSessionRestorer: UserSessionRestorer,
  private val userSessionManager: UserSessionManager,
  private val userGraphManager: UserGraphManager,
) : UserInitializer {

  override suspend fun initialize() {
    val userSession = userSessionRestorer.restore()
    userGraphManager.create(userSession)
    userSessionManager.current = userSession
  }
}
