// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.local

import app.campfire.CampfireDatabase
import app.campfire.auth.di.NewUser
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@NewUser
@ContributesBinding(AppScope::class)
@Inject
class NewUserStorageStrategy(
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
      // The account may already be on the device (signed in again from Add account, or restored).
      // Its rows are then refreshed in place, keeping its data; only the selected library resets,
      // as on any fresh sign-in.
      db.serversQueries.insertOrIgnore(
        serverSettings.asDatabaseModel(
          url = serverUrl,
          userId = user.id,
          name = serverName,
        ),
      )
      db.usersQueries.insertOrIgnore(
        user.asDatabaseModel(serverUrl, userDefaultLibraryId),
      )
      db.usersQueries.updateSelectedLibrary(libraryId = userDefaultLibraryId, userId = user.id)
      db.updateAccount(serverName, serverUrl, serverSettings, user)
    }
  }
}
