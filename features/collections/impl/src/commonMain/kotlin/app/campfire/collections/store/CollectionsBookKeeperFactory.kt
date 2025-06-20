package app.campfire.collections.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.collections.store.CollectionsStore.Operation
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.session.userId
import app.campfire.data.CollectionsBookkeeping
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.Bookkeeper

class CollectionsBookKeeperFactory(
  private val userSessionManager: UserSessionManager,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): Bookkeeper<Operation> {
    return Bookkeeper.by(
      getLastFailedSync = { operation -> getLastFailedSync(operation) },
      setLastFailedSync = { operation, time ->
        require(operation is Operation.Mutation)
        setFailedSync(operation, time)
        true
      },
      clear = { operation -> clear(operation) },
      clearAll = {
        val currentUserId = userSessionManager.current.userId ?: return@by false
        withContext(dispatcherProvider.databaseWrite) {
          db.bookmarkBookkeepingQueries.deleteAll(currentUserId)
        }
        true
      },
    )
  }

  private suspend fun getLastFailedSync(
    operation: Operation,
  ): Long? = withContext(dispatcherProvider.databaseRead) {
    when (operation) {
      is Operation.All -> {
        db.collectionsBookkeepingQueries
          .getLastFailedSyncs(operation.userId)
          .executeAsOneOrNull()
          ?.MAX
      }

      is Operation.Single -> {
        db.collectionsBookkeepingQueries
          .getLastFailedSync(operation.collectionId, operation.userId)
          .executeAsOneOrNull()
          ?.MAX
      }

      is Operation.Mutation -> {
        db.collectionsBookkeepingQueries
          .getLastFailedSyncForOperation(operation.key, operation.userId)
          .executeAsOneOrNull()
          ?.MAX
      }
    }
  }

  private suspend fun setFailedSync(
    mutation: Operation.Mutation,
    time: Long,
  ) = withContext(dispatcherProvider.databaseWrite) {
    when (mutation) {
      is Operation.Mutation.Delete -> {
        db.collectionsBookkeepingQueries
          .insertFailedSync(
            CollectionsBookkeeping(
              userId = mutation.userId,
              collectionId = mutation.id,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.Update -> {
        db.collectionsBookkeepingQueries
          .insertFailedSync(
            CollectionsBookkeeping(
              userId = mutation.userId,
              collectionId = mutation.id,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.Create -> {
        db.collectionsBookkeepingQueries
          .insertFailedSync(
            CollectionsBookkeeping(
              userId = mutation.userId,
              collectionId = mutation.creationId.toHexDashString(),
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.Add -> {
        db.collectionsBookkeepingQueries
          .insertFailedSync(
            CollectionsBookkeeping(
              userId = mutation.userId,
              collectionId = mutation.collectionId,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.Remove -> {
        db.collectionsBookkeepingQueries
          .insertFailedSync(
            CollectionsBookkeeping(
              userId = mutation.userId,
              collectionId = mutation.collectionId,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }
    }
  }

  private suspend fun clear(
    operation: Operation,
  ) = withContext(dispatcherProvider.databaseWrite) {
    when (operation) {
      is Operation.All -> {
        db.collectionsBookkeepingQueries
          .deleteAll(operation.userId)

        true
      }

      is Operation.Single -> {
        db.collectionsBookkeepingQueries
          .deleteFor(operation.userId, operation.collectionId)

        true
      }

      else -> false
    }
  }
}
