package app.campfire.search

import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.search.api.SearchRepository
import app.campfire.search.api.SearchResult
import app.campfire.search.store.SearchStore
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreSearchRepository(
  private val userRepository: UserRepository,
  private val searchStoreFactory: SearchStore.Factory,
) : SearchRepository {

  private val store by lazy { searchStoreFactory.create() }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun searchCurrentLibrary(query: String): Flow<SearchResult> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val key = SearchStore.Operation.Query(
          userId = user.id,
          libraryId = user.selectedLibraryId,
          text = query,
        )
        val request = StoreReadRequest.freshWithFallBackToSourceOfTruth(key)
        store.stream(request)
          .filterNot { it is StoreReadResponse.NoNewData }
          .mapLatest { response ->
            when (response) {
              is StoreReadResponse.Loading -> SearchResult.Loading
              is StoreReadResponse.Error -> SearchResult.Error
              is StoreReadResponse.Data -> response.requireData()
              else -> SearchResult.Error
            }
          }
      }
  }
}
