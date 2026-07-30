// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.socket

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.User
import app.campfire.network.models.User as NetworkUser
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Handles `UserUpdated` socket events by writing through to the local DB. Mirrors the existing
 * `StoreUserRepository` writer: an `UPDATE` keyed by the user id touches only the columns the
 * wire model owns (permissions, locked flag, accessible libraries/tags, etc.). The user row
 * must already exist locally — this event fires for the currently-logged-in user, so it always
 * does.
 *
 * `UserSessionClosed` is intentionally **not** handled here: the client already manages its own
 * playback-session lifecycle (creation, heartbeat, close) and reacting to the server-side close
 * event would race with in-flight client state.
 */
interface UserEventHandler {
  suspend fun onUserUpdated(user: NetworkUser)
}

@ContributesBinding(UserScope::class)
@Inject
class DefaultUserEventHandler(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : UserEventHandler {

  override suspend fun onUserUpdated(user: NetworkUser) {
    withContext(dispatcherProvider.databaseWrite) {
      db.usersQueries.update(
        name = user.username,
        type = User.Type.from(user.type),
        seriesHideFromContinueListening = user.seriesHideFromContinueListening,
        isActive = user.isActive,
        isLocked = user.isLocked,
        lastSeen = user.lastSeen,
        createdAt = user.createdAt,
        permission_download = user.permissions.download,
        permission_upload = user.permissions.upload,
        permission_delete = user.permissions.delete,
        permission_update = user.permissions.update,
        permission_accessAllLibraries = user.permissions.accessAllLibraries,
        permission_accessExplicitContent = user.permissions.accessExplicitContent,
        permission_accessAllTags = user.permissions.accessAllTags,
        librariesAccessible = user.librariesAccessible,
        itemTagsAccessible = user.itemTagsAccessible ?: emptyList(),
        id = user.id,
      )
    }
  }
}
