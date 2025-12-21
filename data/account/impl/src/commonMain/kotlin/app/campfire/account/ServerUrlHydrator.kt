package app.campfire.account

import app.campfire.account.api.UrlHydrator
import app.campfire.core.di.UserScope
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class ServerUrlHydrator(
  private val userSession: UserSession,
) : UrlHydrator {

  override fun hydrateUrl(absolutePath: String): String {
    return "${userSession.serverUrl}$absolutePath"
  }

  override suspend fun hydrateLibraryItem(libraryItemId: LibraryItemId): String {
    return "${userSession.serverUrl}/api/items/$libraryItemId/cover"
  }

  override suspend fun hydrateAuthor(authorId: AuthorId): String {
    return "${userSession.serverUrl}/api/authors/$authorId/image"
  }
}
