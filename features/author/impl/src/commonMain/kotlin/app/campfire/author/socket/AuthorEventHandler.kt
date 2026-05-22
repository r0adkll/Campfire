package app.campfire.author.socket

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.AuthorId
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.Author
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Handles `Author*` socket events by writing through to the local DB.
 *
 * The authors table uses `INSERT OR REPLACE` so a single `insert` covers both Add and Update.
 * Remove uses the existing `deleteForId` query (the wire `AuthorRemovedPayload` only carries the
 * id; library scoping is unnecessary because author ids are globally unique on the server).
 */
interface AuthorEventHandler {
  suspend fun onAuthorAdded(author: Author)
  suspend fun onAuthorUpdated(author: Author)
  suspend fun onAuthorRemoved(authorId: AuthorId)
}

@ContributesBinding(UserScope::class)
@Inject
class DefaultAuthorEventHandler(
  private val db: CampfireDatabase,
  private val urlHydrator: UrlHydrator,
  private val dispatcherProvider: DispatcherProvider,
) : AuthorEventHandler {

  override suspend fun onAuthorAdded(author: Author) = upsert(author)

  override suspend fun onAuthorUpdated(author: Author) = upsert(author)

  override suspend fun onAuthorRemoved(authorId: AuthorId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.authorsQueries.deleteForId(authorId)
    }
  }

  private suspend fun upsert(author: Author) {
    val dbModel = author.asDbModel(urlHydrator)
    withContext(dispatcherProvider.databaseWrite) {
      db.authorsQueries.insert(dbModel)
    }
  }
}
