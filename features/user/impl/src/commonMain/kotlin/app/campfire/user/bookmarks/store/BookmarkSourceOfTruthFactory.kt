package app.campfire.user.bookmarks.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.core.time.FatherTime
import app.campfire.data.Bookmarks
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

class BookmarkSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
  private val fatherTime: FatherTime,
) {

  fun create(): SourceOfTruth<BookmarkStore.Operation, List<Bookmark>, List<Bookmark>> {
    return SourceOfTruth.of(
      reader = { operation ->
        require(operation is BookmarkStore.Operation.Item)
        observeForItem(operation.userId, operation.libraryItemId)
      },
      writer = { operation, bookmarks -> handleWrite(operation, bookmarks) },
      delete = { operation -> handleDelete(operation) },
    )
  }

  private fun observeForItem(userId: UserId, libraryItemId: LibraryItemId): Flow<List<Bookmark>> {
    return db.bookmarksQueries.selectForItem(userId, libraryItemId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { bookmarks ->
        bookmarks.map { it.asDomainModel() }
      }
  }

  private suspend fun handleWrite(
    operation: BookmarkStore.Operation,
    bookmarks: List<Bookmark> = emptyList(),
  ) {
    when (operation) {
      is BookmarkStore.Operation.Item -> writeAll(bookmarks)

      is BookmarkStore.Operation.Mutation.Create -> createOne(
        userId = operation.userId,
        libraryItemId = operation.libraryItemId,
        timeInSeconds = operation.timeInSeconds,
        title = operation.title,
      )

      else -> handleDelete(operation)
    }
  }

  private suspend fun handleDelete(
    operation: BookmarkStore.Operation,
  ) {
    when (operation) {
      is BookmarkStore.Operation.Item -> deleteAll(
        userId = operation.userId,
        libraryItemId = operation.libraryItemId,
      )

      is BookmarkStore.Operation.Mutation.Create -> deleteOne(
        userId = operation.userId,
        libraryItemId = operation.libraryItemId,
        timeInSeconds = operation.timeInSeconds,
      )

      is BookmarkStore.Operation.Mutation.Delete -> deleteOne(
        userId = operation.userId,
        libraryItemId = operation.libraryItemId,
        timeInSeconds = operation.timeInSeconds,
      )
    }
  }

  private suspend fun writeAll(
    bookmarks: List<Bookmark>,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.bookmarksQueries.transaction {
      bookmarks.forEach { bookmark ->
        db.bookmarksQueries.insert(
          bookmark.asDbModel(),
        )
      }
    }
  }

  private suspend fun createOne(
    userId: UserId,
    libraryItemId: LibraryItemId,
    timeInSeconds: Int,
    title: String,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.bookmarksQueries.insert(
      Bookmarks(
        userId = userId,
        libraryItemId = libraryItemId,
        title = title,
        timeInSeconds = timeInSeconds,
        createdAt = fatherTime.now(),
      ),
    )
  }

  private suspend fun deleteOne(
    userId: UserId,
    libraryItemId: LibraryItemId,
    timeInSeconds: Int,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.bookmarksQueries.delete(userId, libraryItemId, timeInSeconds)
  }

  private suspend fun deleteAll(
    userId: UserId,
    libraryItemId: LibraryItemId,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.bookmarksQueries.deleteForItem(userId, libraryItemId)
  }
}
