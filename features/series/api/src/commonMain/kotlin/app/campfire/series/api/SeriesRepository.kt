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
   * The locally cached series listing (with owned books hydrated) — a pure
   * database read that never fetches or writes, so bulk consumers like the
   * upcoming scan can snapshot it without invalidating the series list pager.
   * Empty when nothing has been cached yet.
   */
  suspend fun cachedAllSeries(): List<Series>

  /**
   * Fetches the full series listing from the server and returns it, falling
   * back to the cached listing when the fetch fails. Refreshing rewrites the
   * local series cache, which resets the series list screen's pagination —
   * reserve it for explicit user-initiated refreshes.
   */
  suspend fun refreshAllSeries(): List<Series>

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
