package app.campfire.user.bookmarks.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.core.session.userId
import app.campfire.data.BookmarkFailedCreate
import app.campfire.data.BookmarkFailedDelete
import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.Bookkeeper

class BookmarkBookkeeperFactory(
  private val userSessionManager: UserSessionManager,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): Bookkeeper<BookmarkStore.Operation> {
    return Bookkeeper.by(
      getLastFailedSync = { operation ->
        require(operation is BookmarkStore.Operation.Item)
        getLastFailedSync(operation.userId, operation.libraryItemId)
      },
      setLastFailedSync = { operation, timestamp ->
        require(operation is BookmarkStore.Operation.Mutation)
        setLastFailedSync(operation, timestamp)
        true
      },
      clear = { operation ->
        clear(operation)
        true
      },
      clearAll = {
        val currentUserId = userSessionManager.current.userId ?: return@by false
        withContext(dispatcherProvider.databaseWrite) {
          db.bookmarkBookkeepingQueries.deleteAll(currentUserId)
        }
        true
      },
    )
  }

  private suspend fun getLastFailedSync(userId: UserId, libraryItemId: LibraryItemId): Long? {
    val failedCreates = db.bookmarkBookkeepingQueries.selectFailedCreate(userId, libraryItemId).awaitAsList()
    val failedDeletes = db.bookmarkBookkeepingQueries.selectFailedDelete(userId, libraryItemId).awaitAsList()

    return failedCreates.firstOrNull() ?: failedDeletes.firstOrNull()
  }

  private suspend fun setLastFailedSync(
    operation: BookmarkStore.Operation.Mutation,
    timestamp: Long,
  ) = withContext(dispatcherProvider.databaseWrite) {
    when (operation) {
      is BookmarkStore.Operation.Mutation.Create -> db.bookmarkBookkeepingQueries.insertFailedCreate(
        BookmarkFailedCreate(
          operation.userId,
          operation.libraryItemId,
          operation.timeInSeconds,
          timestamp,
        ),
      )

      is BookmarkStore.Operation.Mutation.Delete -> db.bookmarkBookkeepingQueries.insertFailedDelete(
        BookmarkFailedDelete(
          operation.userId,
          operation.libraryItemId,
          operation.timeInSeconds,
          timestamp,
        ),
      )
    }
  }

  private suspend fun clear(operation: BookmarkStore.Operation) = withContext(dispatcherProvider.databaseWrite) {
    when (operation) {
      is BookmarkStore.Operation.Item -> {
        db.bookmarkBookkeepingQueries.transaction {
          db.bookmarkBookkeepingQueries.deleteAllFailedCreate(operation.userId, operation.libraryItemId)
          db.bookmarkBookkeepingQueries.deleteAllFailedDelete(operation.userId, operation.libraryItemId)
        }
      }
      is BookmarkStore.Operation.Mutation.Create -> {
        db.bookmarkBookkeepingQueries.deleteFailedCreate(
          userId = operation.userId,
          libraryItemId = operation.libraryItemId,
          timeInSeconds = operation.timeInSeconds,
        )
      }
      is BookmarkStore.Operation.Mutation.Delete -> {
        db.bookmarkBookkeepingQueries.deleteFailedDelete(
          userId = operation.userId,
          libraryItemId = operation.libraryItemId,
          timeInSeconds = operation.timeInSeconds,
        )
      }
    }
  }
}
