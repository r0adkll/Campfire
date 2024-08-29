package app.campfire.account.api

import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId

interface CoverImageHydrator {

  suspend fun hydrateUrl(absolutePath: String): String
  suspend fun hydrateLibraryItem(libraryItemId: LibraryItemId): String
  suspend fun hydrateAuthor(authorId: AuthorId): String
}
