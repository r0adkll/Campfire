package app.campfire.libraries.filtering

import app.campfire.core.di.UserScope
import app.campfire.core.model.FilterData
import app.campfire.libraries.api.filtering.FilteringRepository
import app.campfire.libraries.filtering.store.FilteringStore
import app.campfire.user.api.UserRepository
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

@OptIn(ExperimentalCoroutinesApi::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreFilteringRepository(
  private val userRepository: UserRepository,
  private val filteringStoreFactory: FilteringStore.Factory,
) : FilteringRepository {

  private val store by lazy {
    filteringStoreFactory.create()
  }

  override fun observeFilterData(): Flow<FilterData> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val request = StoreReadRequest.cached(user.selectedLibraryId, refresh = true)
        store.stream(request)
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .map {
            it.dataOrNull() ?: FilterData()
          }
      }
  }
}
