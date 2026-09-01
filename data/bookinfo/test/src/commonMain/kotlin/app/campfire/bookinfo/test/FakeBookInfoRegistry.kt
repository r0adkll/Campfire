// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.test

import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.CommunityInfoState
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderStatus
import app.campfire.bookinfo.api.SeriesFetchResult
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeBookInfoRegistry : BookInfoRegistry {

  val providersFlow = MutableSharedFlow<List<ProviderStatus>>(replay = 1)
  override fun observeProviders(): Flow<List<ProviderStatus>> = providersFlow

  val communityInfoFlow = MutableSharedFlow<CommunityInfoState?>(replay = 1)
  val communityInfoRequests = mutableListOf<Pair<LibraryItem, ProviderId?>>()
  override fun observeCommunityInfo(
    item: LibraryItem,
    preferredProvider: ProviderId?,
  ): Flow<CommunityInfoState?> {
    communityInfoRequests += item to preferredProvider
    return communityInfoFlow
  }

  val seriesEntriesFlow = MutableSharedFlow<LoadState<out SeriesInfoState>>(replay = 1)
  val seriesEntriesRequests = mutableListOf<Pair<String, List<LibraryItem>>>()
  override fun observeSeriesEntries(
    seriesName: String,
    ownedItems: List<LibraryItem>,
  ): Flow<LoadState<out SeriesInfoState>> {
    seriesEntriesRequests += seriesName to ownedItems
    return seriesEntriesFlow
  }

  data class FetchSeriesRequest(
    val seriesName: String,
    val ownedItems: List<LibraryItem>,
    val refresh: Boolean,
  )

  val fetchSeriesResults = mutableMapOf<String, SeriesFetchResult>()
  val fetchSeriesRequests = mutableListOf<FetchSeriesRequest>()

  /** Optional suspension point so tests can hold fetches mid-flight. */
  var fetchSeriesGate: (suspend () -> Unit)? = null

  override suspend fun fetchSeriesEntries(
    seriesName: String,
    ownedItems: List<LibraryItem>,
    refresh: Boolean,
  ): SeriesFetchResult {
    fetchSeriesRequests += FetchSeriesRequest(seriesName, ownedItems, refresh)
    fetchSeriesGate?.invoke()
    return fetchSeriesResults[seriesName] ?: SeriesFetchResult.Unavailable
  }

  var clearCacheCount = 0
  override suspend fun clearCache() {
    clearCacheCount++
  }
}
