package app.campfire.home

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.data.mapping.store.debugLogging
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.model.Shelf
import app.campfire.home.api.model.ShelfId
import app.campfire.home.progress.MediaProgressDataSource
import app.campfire.home.store.home.HomeStore
import app.campfire.home.store.shelf.ShelfStore
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreHomeRepository(
  private val userRepository: UserRepository,
  private val mediaProgressDataSource: MediaProgressDataSource,
  private val homeStoreFactory: HomeStore.Factory,
  private val shelfStoreFactory: ShelfStore.Factory,
) : HomeRepository {

  private val homeStore by lazy { homeStoreFactory.create() }
  private val shelfStore by lazy { shelfStoreFactory.create() }

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeHomeFeed(): Flow<FeedResponse<List<Shelf>>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val key = HomeStore.Key(user.selectedLibraryId)
        val request = StoreReadRequest.cached(key, refresh = true)
        homeStore.stream(request)
          .debugLogging(HomeStore.tag)
          .filterNot { it is StoreReadResponse.NoNewData || it is StoreReadResponse.Loading }
          .mapNotNull { response ->
            when (response) {
              is StoreReadResponse.NoNewData -> null
              is StoreReadResponse.Loading -> null
              is StoreReadResponse.Data<*> -> {
                FeedResponse.Success(response.dataOrNull() ?: emptyList())
              }
              is StoreReadResponse.Error -> {
                // If the error is coming from the Fetcher, then we want to ignore it.
                // Only local-origin errors should be reported to the user since fetcher errors
                // could just be a lack of network.
                if (response.origin is StoreReadResponseOrigin.Fetcher) return@mapNotNull null
                when (response) {
                  is StoreReadResponse.Error.Exception -> {
                    FeedResponse.Error.Exception(response.error) as FeedResponse<List<Shelf>>
                  }
                  is StoreReadResponse.Error.Message -> {
                    FeedResponse.Error.Message(response.message) as FeedResponse<List<Shelf>>
                  }
                }
              }
            }
          }
      }
  }

  override fun observeMediaProgress(libraryItemIds: List<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>> {
    return mediaProgressDataSource.observeMediaProgress(libraryItemIds)
  }

  override fun observeShelf(shelfId: ShelfId, shelfType: ShelfType): Flow<List<ShelfEntity>> {
    val request = StoreReadRequest.cached(ShelfStore.Key(shelfId, shelfType), refresh = false)
    return shelfStore.stream(request)
      .debugLogging(ShelfStore.tag)
      .filterNot { it is StoreReadResponse.NoNewData || it is StoreReadResponse.Loading }
      .map { it.dataOrNull() ?: emptyList() }
  }
}
