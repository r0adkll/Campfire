// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import kotlinx.coroutines.flow.Flow

/**
 * How library content is sorted, laid out and presented.
 */
interface LibraryViewSettings {

  fun setLibraryItemDisplayState(value: ItemDisplayState)
  fun observeLibraryItemDisplayState(): Flow<ItemDisplayState>

  fun setLibraryItemMarqueeEnabled(value: Boolean)
  fun observeLibraryItemMarqueeEnabled(): Flow<Boolean>

  fun setLibrarySortMode(value: ContentSortMode)
  fun observeLibrarySortMode(): Flow<ContentSortMode>

  fun setLibrarySortDirection(value: SortDirection)
  fun observeLibrarySortDirection(): Flow<SortDirection>

  fun setAuthorsSortMode(value: ContentSortMode)
  fun observeAuthorsSortMode(): Flow<ContentSortMode>

  fun setAuthorsSortDirection(value: SortDirection)
  fun observeAuthorsSortDirection(): Flow<SortDirection>

  fun setSeriesSortMode(value: ContentSortMode)
  fun observeSeriesSortMode(): Flow<ContentSortMode>

  fun setSeriesSortDirection(value: SortDirection)
  fun observeSeriesSortDirection(): Flow<SortDirection>

  fun setSeriesDisplayState(value: GroupDisplayState)
  fun observeSeriesDisplayState(): Flow<GroupDisplayState>

  fun setCollectionsDisplayState(value: GroupDisplayState)
  fun observeCollectionsDisplayState(): Flow<GroupDisplayState>

  fun setPlaylistsDisplayState(value: GroupDisplayState)
  fun observePlaylistsDisplayState(): Flow<GroupDisplayState>

  fun setShowConfirmDownload(value: Boolean)
  fun observeShowConfirmDownload(): Flow<Boolean>

  fun setShowTimeInBook(value: Boolean)
  fun observeShowTimeInBook(): Flow<Boolean>

  companion object {
    const val DEFAULT_ITEM_MARQUEE_ENABLED: Boolean = true
    const val DEFAULT_SHOW_CONFIRM_DOWNLOAD: Boolean = true
    const val DEFAULT_SHOW_TIME_IN_BOOK: Boolean = true
  }
}
