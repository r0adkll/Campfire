// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.test

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.series.api.SeriesRepository
import app.campfire.series.api.paging.SeriesPager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeSeriesRepository : SeriesRepository {

  var cachedSeries: List<Series> = emptyList()

  fun setCached(series: List<Series>) {
    cachedSeries = series
  }

  var cachedCalls = 0
  override suspend fun cachedAllSeries(): List<Series> {
    cachedCalls++
    return cachedSeries
  }

  var refreshedSeries: List<Series>? = null
  var refreshCalls = 0
  override suspend fun refreshAllSeries(): List<Series> {
    refreshCalls++
    return refreshedSeries ?: cachedSeries
  }

  val seriesLibraryItemsFlow = MutableSharedFlow<List<LibraryItem>>(replay = 1)
  override fun observeSeriesLibraryItems(seriesId: String): Flow<List<LibraryItem>> {
    return seriesLibraryItemsFlow
  }

  override fun createSeriesPager(
    user: User,
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): SeriesPager {
    TODO("Not yet implemented")
  }

  override fun observeFilteredSeriesCount(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?> {
    TODO("Not yet implemented")
  }
}
