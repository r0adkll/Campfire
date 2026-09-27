// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.filters

import app.campfire.core.di.UserScope
import app.campfire.core.model.FilterData
import app.campfire.filters.store.FilteringStore
import app.campfire.user.api.UserRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(UserScope::class)
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
