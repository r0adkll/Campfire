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

  var libraryItemDisplayState: ItemDisplayState
  fun observeLibraryItemDisplayState(): StateFlow<ItemDisplayState>

  var libraryItemMarqueeEnabled: Boolean
  fun observeLibraryItemMarqueeEnabled(): StateFlow<Boolean>

  var librarySortMode: ContentSortMode
  fun observeLibrarySortMode(): StateFlow<ContentSortMode>

  var librarySortDirection: SortDirection
  fun observeLibrarySortDirection(): StateFlow<SortDirection>

  var authorsSortMode: ContentSortMode
  fun observeAuthorsSortMode(): StateFlow<ContentSortMode>

  var authorsSortDirection: SortDirection
  fun observeAuthorsSortDirection(): StateFlow<SortDirection>

  var seriesSortMode: ContentSortMode
  fun observeSeriesSortMode(): StateFlow<ContentSortMode>

  var seriesSortDirection: SortDirection
  fun observeSeriesSortDirection(): StateFlow<SortDirection>

  var seriesDisplayState: GroupDisplayState
  fun observeSeriesDisplayState(): StateFlow<GroupDisplayState>

  var collectionsDisplayState: GroupDisplayState
  fun observeCollectionsDisplayState(): StateFlow<GroupDisplayState>

  var playlistsDisplayState: GroupDisplayState
  fun observePlaylistsDisplayState(): StateFlow<GroupDisplayState>

  var showConfirmDownload: Boolean
  fun observeShowConfirmDownload(): StateFlow<Boolean>

  var showTimeInBook: Boolean
  fun observeShowTimeInBook(): StateFlow<Boolean>
}
