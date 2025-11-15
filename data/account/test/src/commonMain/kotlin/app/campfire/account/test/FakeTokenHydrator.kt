package app.campfire.account.test

import app.campfire.account.api.TokenHydrator
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId

class FakeTokenHydrator : TokenHydrator {

  override fun hydrateUrl(absolutePath: String): String {
    return absolutePath
  }

  override suspend fun hydrateUrlWithToken(absolutePath: String): String {
    return absolutePath
  }

  override suspend fun hydrateLibraryItem(libraryItemId: LibraryItemId): String {
    return "https://fakeserver.com/library/item/$libraryItemId"
  }

  override suspend fun hydrateAuthor(authorId: AuthorId): String {
    return "https://fakeserver.com/author/$authorId"
  }
}
