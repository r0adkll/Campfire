package app.campfire.account.api

import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId

@Deprecated(
  "We should no longer be appending Urls with the users access token and should instead be passing it as" +
    "a header in w/e network request is being driven."
)
interface TokenHydrator {

  fun hydrateUrl(absolutePath: String): String
  suspend fun hydrateUrlWithToken(absolutePath: String): String
  suspend fun hydrateLibraryItem(libraryItemId: LibraryItemId): String
  suspend fun hydrateAuthor(authorId: AuthorId): String
}
