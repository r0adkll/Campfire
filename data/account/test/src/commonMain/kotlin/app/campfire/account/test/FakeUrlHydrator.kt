// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.test

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId

class FakeUrlHydrator : UrlHydrator {

  override fun hydrateUrl(absolutePath: String): String {
    return absolutePath
  }

  override fun hydrateLibraryItem(libraryItemId: LibraryItemId): String {
    return "https://fakeserver.com/library/item/$libraryItemId"
  }

  override fun hydrateAuthor(authorId: AuthorId): String {
    return "https://fakeserver.com/author/$authorId"
  }
}
