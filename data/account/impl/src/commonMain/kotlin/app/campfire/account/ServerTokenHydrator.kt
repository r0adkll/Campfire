package app.campfire.account

import app.campfire.account.api.TokenHydrator
import app.campfire.account.storage.TokenStorage
import app.campfire.core.di.UserScope
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.core.session.userId
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(UserScope::class)
@Inject
class ServerTokenHydrator(
  private val userSession: UserSession,
  private val tokenStorage: TokenStorage,
) : TokenHydrator {

  override fun hydrateUrl(absolutePath: String): String {
    return "${userSession.serverUrl}$absolutePath"
  }

  override suspend fun hydrateUrlWithToken(absolutePath: String): String {
    return "${userSession.serverUrl}$absolutePath?token=${getCurrentToken()}"
  }

  override suspend fun hydrateLibraryItem(libraryItemId: LibraryItemId): String {
    return "${userSession.serverUrl}/api/items/$libraryItemId/cover?token=${getCurrentToken()}"
  }

  override suspend fun hydrateAuthor(authorId: AuthorId): String {
    return "${userSession.serverUrl}/api/authors/$authorId/image?token=${getCurrentToken()}"
  }

  private suspend fun getCurrentToken(): String? {
    return userSession.userId?.let { userId -> tokenStorage.get(userId) }
  }
}
