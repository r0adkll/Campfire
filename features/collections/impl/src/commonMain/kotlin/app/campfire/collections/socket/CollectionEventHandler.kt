package app.campfire.collections.socket

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.CollectionId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.Collection
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Handles `Collection*` socket events by writing through to the local DB.
 *
 * Collection rows are owned by a single (user, library) pair and the wire model carries
 * everything we persist locally, so `INSERT OR REPLACE` (the `insert` query) is enough.
 */
interface CollectionEventHandler {
  suspend fun onCollectionAdded(collection: Collection)
  suspend fun onCollectionUpdated(collection: Collection)
  suspend fun onCollectionRemoved(collectionId: CollectionId)
}

@ContributesBinding(UserScope::class)
@Inject
class DefaultCollectionEventHandler(
  private val db: CampfireDatabase,
  private val userSession: UserSession,
  private val dispatcherProvider: DispatcherProvider,
) : CollectionEventHandler {

  override suspend fun onCollectionAdded(collection: Collection) = upsert(collection)

  override suspend fun onCollectionUpdated(collection: Collection) = upsert(collection)

  override suspend fun onCollectionRemoved(collectionId: CollectionId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.collectionsQueries.delete(collectionId)
    }
  }

  private suspend fun upsert(collection: Collection) {
    val userId = userSession.userId ?: return
    val dbModel = collection.asDbModel(userId)
    withContext(dispatcherProvider.databaseWrite) {
      db.collectionsQueries.insert(dbModel)
    }
  }
}
