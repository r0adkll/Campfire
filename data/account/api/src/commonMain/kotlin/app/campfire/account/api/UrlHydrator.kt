// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.api

import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId

interface UrlHydrator {

  fun hydrateUrl(absolutePath: String): String
  fun hydrateLibraryItem(libraryItemId: LibraryItemId): String
  fun hydrateAuthor(authorId: AuthorId): String
}
