package app.campfire.home

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.data.mapping.store.debugLogging
import app.campfire.home.api.HomeFeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.progress.MediaProgressDataSource
import app.campfire.home.store.HomeStore
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
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
) : HomeRepository {

  private val homeStore by lazy { homeStoreFactory.create() }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeHomeFeed(): Flow<HomeFeedResponse> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val key = HomeStore.Key(user.selectedLibraryId)
        val request = StoreReadRequest.cached(key, refresh = true)
        homeStore.stream(request)
          .debugLogging("HomeStore")
          .filterNot { it is StoreReadResponse.NoNewData || it is StoreReadResponse.Loading }
          .mapNotNull { response ->
            when (response) {
              is StoreReadResponse.NoNewData -> null
              is StoreReadResponse.Loading -> null
              is StoreReadResponse.Data<*> -> {
                HomeFeedResponse.Success(response.dataOrNull() ?: emptyList())
              }
              is StoreReadResponse.Error -> {
                // If the error is coming from the Fetcher, then we want to ignore it.
                // Only local-origin errors should be reported to the user since fetcher errors
                // could just be a lack of network.
                if (response.origin is StoreReadResponseOrigin.Fetcher) return@mapNotNull null
                when (response) {
                  is StoreReadResponse.Error.Exception -> HomeFeedResponse.Error.Exception(response.error)
                  is StoreReadResponse.Error.Message -> HomeFeedResponse.Error.Message(response.message)
                }
              }
            }
          }
      }
  }

  override fun observeMediaProgress(libraryItemIds: List<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>> {
    return mediaProgressDataSource.observeMediaProgress(libraryItemIds)
  }
}
