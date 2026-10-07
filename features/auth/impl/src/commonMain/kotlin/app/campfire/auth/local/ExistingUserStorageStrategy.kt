// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.local

import app.campfire.CampfireDatabase
import app.campfire.auth.di.ExistingUser
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@ExistingUser
@ContributesBinding(AppScope::class)
@Inject
class ExistingUserStorageStrategy(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : UserStorageStrategy {

  override suspend fun store(
    serverName: String,
    serverUrl: String,
    serverSettings: ServerSettings,
    user: User,
    userDefaultLibraryId: String,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      db.updateAccount(serverName, serverUrl, serverSettings, user)
    }
  }
}
