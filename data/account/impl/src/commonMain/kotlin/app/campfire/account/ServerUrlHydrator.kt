// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account

import app.campfire.account.api.UrlHydrator
import app.campfire.core.di.UserScope
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class ServerUrlHydrator(
  private val userSession: UserSession,
) : UrlHydrator {

  override fun hydrateUrl(absolutePath: String): String {
    return "${userSession.serverUrl}$absolutePath"
  }

  override fun hydrateLibraryItem(libraryItemId: LibraryItemId, updatedAtMillis: Long?): String {
    return "${userSession.serverUrl}/api/items/$libraryItemId/cover".withTimestamp(updatedAtMillis)
  }

  override fun hydrateAuthor(authorId: AuthorId, updatedAtMillis: Long?): String {
    return "${userSession.serverUrl}/api/authors/$authorId/image".withTimestamp(updatedAtMillis)
  }

  private fun String.withTimestamp(updatedAtMillis: Long?): String {
    if (updatedAtMillis == null || updatedAtMillis <= 0L) return this
    return "$this?ts=$updatedAtMillis"
  }
}
