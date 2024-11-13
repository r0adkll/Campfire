package app.campfire.account.api

import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId

interface CoverImageHydrator {

  fun hydrateUrl(absolutePath: String): String
  suspend fun hydrateUrlWithToken(absolutePath: String): String
  suspend fun hydrateLibraryItem(libraryItemId: LibraryItemId): String
  suspend fun hydrateAuthor(authorId: AuthorId): String
}
