// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.test

import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.CommunityInfoState
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderStatus
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeBookInfoRegistry : BookInfoRegistry {

  val providersFlow = MutableSharedFlow<List<ProviderStatus>>(replay = 1)
  override fun observeProviders(): Flow<List<ProviderStatus>> = providersFlow

  val communityInfoFlow = MutableSharedFlow<LoadState<out CommunityInfoState?>>(replay = 1)
  val communityInfoRequests = mutableListOf<Pair<LibraryItem, ProviderId?>>()
  override fun observeCommunityInfo(
    item: LibraryItem,
    preferredProvider: ProviderId?,
  ): Flow<LoadState<out CommunityInfoState?>> {
    communityInfoRequests += item to preferredProvider
    return communityInfoFlow
  }

  var clearCacheCount = 0
  override suspend fun clearCache() {
    clearCacheCount++
  }
}
