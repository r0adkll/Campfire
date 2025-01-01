package app.campfire.user.progress

import app.campfire.CampfireDatabase
import app.campfire.core.model.UserId
import app.campfire.data.MediaProgressFailedDelete
import app.campfire.data.MediaProgressFailedUpdate
import app.campfire.user.progress.MediaProgressStore.Operation
import app.cash.sqldelight.async.coroutines.awaitAsList
import org.mobilenativefoundation.store.store5.Bookkeeper

class MediaProgressBookkeeperFactory(
  private val db: CampfireDatabase,
) {

  fun create(): Bookkeeper<Operation> = Bookkeeper.by(
    getLastFailedSync = { operation ->
      MediaProgressStore.ibark { "Bookkeeper[getLastFailedSync]: --> $operation" }
      require(operation is Operation.Query)
      handleGetLastFailedSync(operation).also { timestamp ->
        MediaProgressStore.ibark { "Bookkeeper[getLastFailedSync]: <-- timestamp=$timestamp" }
      }
    },
    setLastFailedSync = { operation, timestamp ->
      MediaProgressStore.ibark { "Bookkeeper[setLastFailedSync]: $operation, timestamp=$timestamp" }
      require(operation is Operation.Mutation)
      handleSetLastFailedSync(operation, timestamp)
    },
    clear = { operation ->
      MediaProgressStore.ibark { "Bookkeeper[clear]: $operation" }
      handleClear(operation)
    },
    clearAll = {
      MediaProgressStore.ibark { "Bookkeeper[clearAll]" }
      // TODO: This could potentially clobber syncs for other users signed in, we should probably
      //  find a better way to associate user specific data without having to query it out of [UserRepository]
      clearAllFailedSyncs()
      true
    },
  )

  private suspend fun firstFailedSyncOrNull(id: String): Long? {
    val failedUpdates =
      db.mediaProgressBookkeepingQueries
        .getManyFailedUpdates(setOf(id))
        .awaitAsList()
    val failedDeletes =
      db.mediaProgressBookkeepingQueries
        .getManyFailedDeletes(setOf(id))
        .awaitAsList()

    return failedUpdates.firstOrNull()?.timestamp
      ?: failedDeletes.firstOrNull()?.timestamp
  }

  private suspend fun firstFailedSyncOrNullForUser(userId: UserId): Long? {
    val failedUpdates =
      db.mediaProgressBookkeepingQueries
        .getFailedUpdatesForUser(userId)
        .awaitAsList()
    val failedDeletes =
      db.mediaProgressBookkeepingQueries
        .getFailedDeletesForUser(userId)
        .awaitAsList()

    return failedUpdates.firstOrNull()?.timestamp
      ?: failedDeletes.firstOrNull()?.timestamp
  }

  private suspend fun handleGetLastFailedSync(operation: Operation.Query): Long? {
    return when (operation) {
      is Operation.Query.One -> {
        firstFailedSyncOrNull(operation.libraryItemId)
      }

      is Operation.Query.All -> {
        firstFailedSyncOrNullForUser(operation.userId)
      }
    }
  }


  private suspend fun handleSetLastFailedSync(
    operation: Operation.Mutation,
    timestamp: Long,
  ): Boolean = when (operation) {
    is Operation.Mutation.Update -> insertFailedUpdateSync(operation, timestamp)
    is Operation.Mutation.Delete.One -> {
      db.mediaProgressBookkeepingQueries
        .insertFailedDelete(
          MediaProgressFailedDelete(
            libraryItemId = operation.libraryItemId,
            userId = operation.userId,
            timestamp = timestamp,
          )
        )
      true
    }
  }

  private suspend fun insertFailedUpdateSync(
    operation: Operation.Mutation.Update,
    timestamp: Long,
  ): Boolean {
    return when (operation) {
      is Operation.Mutation.Update.UpsertOne -> {
        db.mediaProgressBookkeepingQueries
          .insertFailedUpdate(
            MediaProgressFailedUpdate(
              libraryItemId = operation.item.libraryItemId,
              userId = operation.item.userId,
              timestamp = timestamp,
            ),
          )
        true
      }

      is Operation.Mutation.Update.UpsertMany -> {
        db.mediaProgressBookkeepingQueries.transactionWithResult {
          operation.items.forEach { item ->
            db.mediaProgressBookkeepingQueries
              .insertFailedUpdate(
                MediaProgressFailedUpdate(
                  libraryItemId = item.libraryItemId,
                  userId = item.userId,
                  timestamp = timestamp,
                ),
              )
          }
          true
        }
      }
    }
  }

  private suspend fun handleClear(operation: Operation): Boolean = when(operation) {
    is Operation.Query.All -> {
      clearFailedSyncsForUser(operation.userId)
      true
    }

    is Operation.Query.One -> {
      clearFailedSyncs(operation.libraryItemId)
      true
    }

    is Operation.Mutation.Delete.One -> {
      clearFailedSyncs(operation.libraryItemId)
      true
    }

    is Operation.Mutation.Update.UpsertMany -> {
      db.mediaProgressBookkeepingQueries.transactionWithResult {
        operation.items.forEach { item ->
          clearFailedSyncs(item.libraryItemId)
        }
        true
      }
    }

    is Operation.Mutation.Update.UpsertOne -> {
      clearFailedSyncs(operation.item.libraryItemId)
      true
    }
  }

  private suspend fun clearFailedSyncs(libraryItemId: String) {
    db.mediaProgressBookkeepingQueries.clearFailedUpdates(libraryItemId)
    db.mediaProgressBookkeepingQueries.clearFailedDeletes(libraryItemId)
  }

  private suspend fun clearFailedSyncsForUser(userId: UserId) {
    db.mediaProgressBookkeepingQueries.clearAllFailedUpdatesForUser(userId)
    db.mediaProgressBookkeepingQueries.clearAllFailedDeletesForUser(userId)
  }

  private suspend fun clearAllFailedSyncs() {
    db.mediaProgressBookkeepingQueries.clearAllFailedUpdates()
    db.mediaProgressBookkeepingQueries.clearAllFailedDeletes()
  }
}
