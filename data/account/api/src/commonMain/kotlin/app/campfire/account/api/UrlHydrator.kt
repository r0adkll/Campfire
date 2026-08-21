// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.api

import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId

interface UrlHydrator {

  fun hydrateUrl(absolutePath: String): String

  /**
   * Absolute URL for a library item's cover. When [updatedAtMillis] is known it is appended as a `ts`
   * query parameter (mirroring the Audiobookshelf web client) so image caches refresh after the server
   * reports a change instead of serving a stale cover indefinitely.
   */
  fun hydrateLibraryItem(libraryItemId: LibraryItemId, updatedAtMillis: Long? = null): String

  /** Absolute URL for an author's image; see [hydrateLibraryItem] for [updatedAtMillis]. */
  fun hydrateAuthor(authorId: AuthorId, updatedAtMillis: Long? = null): String
}
