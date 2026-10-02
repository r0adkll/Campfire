// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.settings.api.LibraryViewSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestLibraryViewSettings(
  private val testScope: CoroutineScope = TestScope(UnconfinedTestDispatcher()),
) : TestSettings(), LibraryViewSettings {

  override var libraryItemDisplayState: ItemDisplayState by enum()
  override fun observeLibraryItemDisplayState(): StateFlow<ItemDisplayState> =
    observeEnum<ItemDisplayState>(::libraryItemDisplayState)
      .stateIn(testScope, SharingStarted.Lazily, libraryItemDisplayState)

  override var libraryItemMarqueeEnabled: Boolean by boolean()
  override fun observeLibraryItemMarqueeEnabled(): StateFlow<Boolean> =
    observeBoolean(::libraryItemMarqueeEnabled)
      .stateIn(testScope, SharingStarted.Lazily, libraryItemMarqueeEnabled)

  override var librarySortMode: ContentSortMode by enum()
  override fun observeLibrarySortMode(): StateFlow<ContentSortMode> =
    observeEnum<ContentSortMode>(::librarySortMode)
      .stateIn(testScope, SharingStarted.Lazily, librarySortMode)

  override var librarySortDirection: SortDirection by enum()
  override fun observeLibrarySortDirection(): StateFlow<SortDirection> =
    observeEnum<SortDirection>(::librarySortDirection)
      .stateIn(testScope, SharingStarted.Lazily, librarySortDirection)

  override var authorsSortMode: ContentSortMode by enum()
  override fun observeAuthorsSortMode(): StateFlow<ContentSortMode> =
    observeEnum<ContentSortMode>(::authorsSortMode)
      .stateIn(testScope, SharingStarted.Lazily, authorsSortMode)

  override var authorsSortDirection: SortDirection by enum()
  override fun observeAuthorsSortDirection(): StateFlow<SortDirection> =
    observeEnum<SortDirection>(::authorsSortDirection)
      .stateIn(testScope, SharingStarted.Lazily, authorsSortDirection)

  override var seriesSortMode: ContentSortMode by enum()
  override fun observeSeriesSortMode(): StateFlow<ContentSortMode> =
    observeEnum<ContentSortMode>(::seriesSortMode)
      .stateIn(testScope, SharingStarted.Lazily, seriesSortMode)

  override var seriesSortDirection: SortDirection by enum()
  override fun observeSeriesSortDirection(): StateFlow<SortDirection> =
    observeEnum<SortDirection>(::seriesSortDirection)
      .stateIn(testScope, SharingStarted.Lazily, seriesSortDirection)

  override var seriesDisplayState: GroupDisplayState by enum()
  override fun observeSeriesDisplayState(): StateFlow<GroupDisplayState> =
    observeEnum<GroupDisplayState>(::seriesDisplayState)
      .stateIn(testScope, SharingStarted.Lazily, seriesDisplayState)

  override var collectionsDisplayState: GroupDisplayState by enum()
  override fun observeCollectionsDisplayState(): StateFlow<GroupDisplayState> =
    observeEnum<GroupDisplayState>(::collectionsDisplayState)
      .stateIn(testScope, SharingStarted.Lazily, collectionsDisplayState)

  override var playlistsDisplayState: GroupDisplayState by enum()
  override fun observePlaylistsDisplayState(): StateFlow<GroupDisplayState> =
    observeEnum<GroupDisplayState>(::playlistsDisplayState)
      .stateIn(testScope, SharingStarted.Lazily, playlistsDisplayState)

  override var showConfirmDownload: Boolean by boolean()
  override fun observeShowConfirmDownload(): StateFlow<Boolean> =
    observeBoolean(::showConfirmDownload)
      .stateIn(testScope, SharingStarted.Lazily, showConfirmDownload)

  override var showTimeInBook: Boolean by boolean()
  override fun observeShowTimeInBook(): StateFlow<Boolean> =
    observeBoolean(::showTimeInBook)
      .stateIn(testScope, SharingStarted.Lazily, showTimeInBook)
}
