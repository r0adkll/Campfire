package app.campfire.user.bookmarks

import app.campfire.account.api.UserSessionManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.Bookmark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.user.api.BookmarkRepository
import app.campfire.user.bookmarks.store.BookmarkStore
import app.campfire.user.bookmarks.store.BookmarkStore.Operation.Mutation.Create
import app.campfire.user.bookmarks.store.BookmarkStore.Operation.Mutation.Delete
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreWriteRequest

@OptIn(ExperimentalStoreApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class StoreBookmarkRepository(
  private val storeFactory: BookmarkStore.Factory,
  private val userSessionManager: UserSessionManager,
) : BookmarkRepository {

  private val bookmarkStore by lazy { storeFactory.create() }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeBookmarks(libraryItemId: LibraryItemId): Flow<List<Bookmark>> {
    return userSessionManager.observe()
      .filterIsInstance<UserSession.LoggedIn>()
      .flatMapLatest { session ->
        val request = StoreReadRequest.cached(
          BookmarkStore.Operation.Item(session.user.id, libraryItemId),
          refresh = true,
        )

        bookmarkStore.stream<List<Bookmark>>(request)
          .onEach { BookmarkStore.dbark { "observe --> $it" } }
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .map { it.dataOrNull() ?: emptyList() }
          .map { it.sortedBy(Bookmark::time) }
      }
  }

  override suspend fun createBookmark(libraryItemId: LibraryItemId, timestamp: Duration, title: String) {
    val currentUserId = userSessionManager.current.userId ?: return

    val request = StoreWriteRequest.of<BookmarkStore.Operation, List<Bookmark>, BookmarkStore.Update>(
      key = Create(currentUserId, libraryItemId, timestamp.inWholeSeconds.toInt(), title),
      value = emptyList(),
    )
    val response = bookmarkStore.write(request)
    BookmarkStore.ibark { "create --> $response" }
  }

  override suspend fun removeBookmark(libraryItemId: LibraryItemId, timestamp: Duration) {
    val currentUserId = userSessionManager.current.userId ?: return

    val request = StoreWriteRequest.of<BookmarkStore.Operation, List<Bookmark>, BookmarkStore.Update>(
      key = Delete(currentUserId, libraryItemId, timestamp.inWholeSeconds.toInt()),
      value = emptyList(),
    )
    val response = bookmarkStore.write(request)
    BookmarkStore.ibark { "delete --> $response" }
  }
}
