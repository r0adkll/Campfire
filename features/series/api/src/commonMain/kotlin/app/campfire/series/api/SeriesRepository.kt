// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.api

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.series.api.paging.SeriesPager
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {

  /**
   * The locally cached series listing (with owned books hydrated). With
   * [refresh] the listing is also refetched from the server, but cached series
   * emit before that fetch lands — and the cache only holds series loaded
   * somewhere (series pages, shelves, search) — so a first emission isn't the
   * complete listing. Use [getAllSeries] for that.
   */
  fun observeAllSeries(refresh: Boolean = true): Flow<List<Series>>

  /**
   * The current library's complete series listing (with owned books
   * hydrated), fetched from the server and written through to the cache.
   * Falls back to the cached listing when the fetch fails (e.g. offline).
   */
  suspend fun getAllSeries(): List<Series>

  fun createSeriesPager(
    user: User,
    filter: ContentFilter? = null,
    sortMode: ContentSortMode = ContentSortMode.Name,
    sortDirection: SortDirection = SortDirection.Default,
  ): SeriesPager

  fun observeFilteredSeriesCount(
    filter: ContentFilter? = null,
    sortMode: ContentSortMode = ContentSortMode.Name,
    sortDirection: SortDirection = SortDirection.Default,
  ): Flow<Int?>

  fun observeSeriesLibraryItems(seriesId: String): Flow<List<LibraryItem>>
}
