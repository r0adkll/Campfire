package app.campfire.account

import app.campfire.account.api.CoverImageHydrator
import app.campfire.account.storage.TokenStorage
import app.campfire.core.di.UserScope
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

// TODO: Kimchi bug, this tried to solve WITHOUT @Inject annotation and parameters
@ContributesBinding(UserScope::class)
@Inject
class ServerCoverImageHydrator(
  private val userSession: UserSession,
  private val tokenStorage: TokenStorage,
) : CoverImageHydrator {

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
    return userSession.serverUrl?.let { tokenStorage.get(it) }
  }
}
