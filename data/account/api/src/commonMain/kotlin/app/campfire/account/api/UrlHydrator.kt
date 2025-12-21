package app.campfire.account.api

import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId

interface UrlHydrator {

  fun hydrateUrl(absolutePath: String): String
  suspend fun hydrateLibraryItem(libraryItemId: LibraryItemId): String
  suspend fun hydrateAuthor(authorId: AuthorId): String
}
