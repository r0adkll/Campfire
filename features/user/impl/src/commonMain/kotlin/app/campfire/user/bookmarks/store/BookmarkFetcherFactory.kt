package app.campfire.user.bookmarks.store

import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.User
import org.mobilenativefoundation.store.store5.Fetcher

class BookmarkFetcherFactory(
  private val api: AudioBookShelfApi,
) {

  fun create(): Fetcher<BookmarkStore.Operation, User> {
    return Fetcher.ofResult { operation ->
      require(operation is BookmarkStore.Operation.Item)
      api.getCurrentUser()
        .also {
          BookmarkStore.dbark { "Fetcher Result(${it.getOrNull()?.bookmarks})" }
        }
        .asFetcherResult()
    }
  }
}
