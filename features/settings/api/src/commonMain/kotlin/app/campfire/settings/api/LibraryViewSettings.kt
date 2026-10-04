// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import kotlinx.coroutines.flow.StateFlow

/**
 * How library content is sorted, laid out and presented.
 */
interface LibraryViewSettings {

  val libraryItemDisplayState: ItemDisplayState
  fun setLibraryItemDisplayState(value: ItemDisplayState)
  fun observeLibraryItemDisplayState(): StateFlow<ItemDisplayState>

  val libraryItemMarqueeEnabled: Boolean
  fun setLibraryItemMarqueeEnabled(value: Boolean)
  fun observeLibraryItemMarqueeEnabled(): StateFlow<Boolean>

  val librarySortMode: ContentSortMode
  fun setLibrarySortMode(value: ContentSortMode)
  fun observeLibrarySortMode(): StateFlow<ContentSortMode>

  val librarySortDirection: SortDirection
  fun setLibrarySortDirection(value: SortDirection)
  fun observeLibrarySortDirection(): StateFlow<SortDirection>

  val authorsSortMode: ContentSortMode
  fun setAuthorsSortMode(value: ContentSortMode)
  fun observeAuthorsSortMode(): StateFlow<ContentSortMode>

  val authorsSortDirection: SortDirection
  fun setAuthorsSortDirection(value: SortDirection)
  fun observeAuthorsSortDirection(): StateFlow<SortDirection>

  val seriesSortMode: ContentSortMode
  fun setSeriesSortMode(value: ContentSortMode)
  fun observeSeriesSortMode(): StateFlow<ContentSortMode>

  val seriesSortDirection: SortDirection
  fun setSeriesSortDirection(value: SortDirection)
  fun observeSeriesSortDirection(): StateFlow<SortDirection>

  val seriesDisplayState: GroupDisplayState
  fun setSeriesDisplayState(value: GroupDisplayState)
  fun observeSeriesDisplayState(): StateFlow<GroupDisplayState>

  val collectionsDisplayState: GroupDisplayState
  fun setCollectionsDisplayState(value: GroupDisplayState)
  fun observeCollectionsDisplayState(): StateFlow<GroupDisplayState>

  val playlistsDisplayState: GroupDisplayState
  fun setPlaylistsDisplayState(value: GroupDisplayState)
  fun observePlaylistsDisplayState(): StateFlow<GroupDisplayState>

  val showConfirmDownload: Boolean
  fun setShowConfirmDownload(value: Boolean)
  fun observeShowConfirmDownload(): StateFlow<Boolean>

  val showTimeInBook: Boolean
  fun setShowTimeInBook(value: Boolean)
  fun observeShowTimeInBook(): StateFlow<Boolean>
}
