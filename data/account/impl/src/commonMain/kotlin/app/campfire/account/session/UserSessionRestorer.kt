// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.session

import app.campfire.CampfireDatabase
import app.campfire.account.server.db.ServerWithUser
import app.campfire.account.storage.TokenStorage
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.bark
import app.campfire.core.session.UserSession
import app.campfire.settings.api.DeviceSettings
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.measureTimedValue
import kotlinx.coroutines.withContext

interface UserSessionRestorer {

  suspend fun restore(): UserSession
}

@ContributesBinding(AppScope::class)
@Inject
class DatabaseUserSessionRestorer(
  private val tokenStorage: TokenStorage,
  private val deviceSettings: DeviceSettings,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : UserSessionRestorer {

  override suspend fun restore(): UserSession = measureTimedValue {
    val currentUserId = deviceSettings.currentUserId ?: return@measureTimedValue UserSession.LoggedOut

    val server = withContext(dispatcherProvider.databaseRead) {
      db.serversQueries.selectByUserId(currentUserId, ::ServerWithUser)
        .awaitAsOneOrNull()
        ?.asDomainModel()
    }

    if (server == null) {
      deviceSettings.currentUserId = null
      return@measureTimedValue UserSession.LoggedOut
    }

    // Only check that a token is stored: reading it means decrypting it, which on Android is a
    // round of Android Keystore calls that would hold up the main thread waiting on this restore.
    // A stored token that turns out to be unreadable fails its first request, and the refresh
    // that follows asks the user to sign in again.
    if (tokenStorage.has(server.user.id)) {
      return@measureTimedValue UserSession.LoggedIn(server.user)
    } else {
      return@measureTimedValue UserSession.NeedsAuthentication(server)
    }
  }.let { timedValue ->
    bark("UserSessionRestorer") {
      "Restored ${timedValue.value::class.simpleName} in ${timedValue.duration}"
    }
    timedValue.value
  }
}
