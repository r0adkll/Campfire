package app.campfire.account

import app.campfire.account.api.TokenHydrator
import app.campfire.account.storage.TokenStorage
import app.campfire.core.di.UserScope
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.core.session.userId
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class ServerTokenHydrator(
  private val userSession: UserSession,
  private val tokenStorage: TokenStorage,
) : TokenHydrator {

  private var cachedToken: String? = null

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
    if (cachedToken != null) return cachedToken
    return userSession.userId?.let { userId ->
      tokenStorage.get(userId).also { token ->
        /*
         * This token is not going to change mid-usersession
         * so lets cache it for faster use
         */
        cachedToken = token
      }
    }
  }
}
