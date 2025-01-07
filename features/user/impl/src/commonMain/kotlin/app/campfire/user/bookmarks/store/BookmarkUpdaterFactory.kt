package app.campfire.user.bookmarks.store

import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.UserId
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.AudioBookShelfApi
import org.mobilenativefoundation.store.store5.OnUpdaterCompletion
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

class BookmarkUpdaterFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Updater<BookmarkStore.Operation, List<Bookmark>, BookmarkStore.Update> {
    return Updater.by(
      post = { operation, bookmarks ->
        BookmarkStore.dbark { "post --> $operation, $bookmarks" }
        handlePost(operation, bookmarks)
      },
      onCompletion = OnUpdaterCompletion(
        onSuccess = {
          BookmarkStore.ibark { "Updater completed successfully: $it" }
        },
        onFailure = {
          BookmarkStore.ebark { "Updater failed: $it" }
        },
      ),
    )
  }

  private suspend fun handlePost(
    operation: BookmarkStore.Operation,
    bookmarks: List<Bookmark>,
  ): UpdaterResult = when (operation) {
    is BookmarkStore.Operation.Item -> {
      bookmarks.forEach {
        val result = createOne(
          it.userId,
          it.libraryItemId,
          it.time.inWholeSeconds.toInt(),
          it.title,
        )

        BookmarkStore.vbark { "posting ~~> B(${it.time.inWholeSeconds}) = $result" }

        if (result is UpdaterResult.Error) {
          return result
        }
      }

      UpdaterResult.Success.Typed(Unit)
    }

    is BookmarkStore.Operation.Mutation.Create -> createOne(
      operation.userId,
      operation.libraryItemId,
      operation.timeInSeconds,
      operation.title,
    )

    is BookmarkStore.Operation.Mutation.Delete -> deleteOne(
      libraryItemId = operation.libraryItemId,
      timeInSeconds = operation.timeInSeconds,
    )
  }

  private suspend fun createOne(
    userId: UserId,
    libraryItemId: LibraryItemId,
    timeInSeconds: Int,
    title: String,
  ): UpdaterResult {
    return api.createBookmark(libraryItemId, timeInSeconds, title).map { it.asDomainModel(userId) }.asUpdaterResult()
  }

  private suspend fun deleteOne(
    libraryItemId: LibraryItemId,
    timeInSeconds: Int,
  ): UpdaterResult {
    return api.removeBookmark(libraryItemId, timeInSeconds).asUpdaterResult()
  }

  private fun <T : Any> Result<T>.asUpdaterResult(): UpdaterResult {
    return when {
      isSuccess -> UpdaterResult.Success.Typed(getOrThrow())
      else -> exceptionOrNull()?.let {
        UpdaterResult.Error.Exception(it)
      } ?: UpdaterResult.Error.Message("Error occurred while updating")
    }
  }
}
