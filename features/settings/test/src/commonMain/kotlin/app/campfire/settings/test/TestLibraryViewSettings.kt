// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.GroupDisplayState
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.settings.api.LibraryViewSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [LibraryViewSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class TestLibraryViewSettings : LibraryViewSettings {

  private val _libraryItemDisplayState = MutableStateFlow<ItemDisplayState>(ItemDisplayState.entries.first())
  val libraryItemDisplayState: ItemDisplayState get() = _libraryItemDisplayState.value
  override fun setLibraryItemDisplayState(value: ItemDisplayState) {
    _libraryItemDisplayState.value = value
  }
  override fun observeLibraryItemDisplayState(): StateFlow<ItemDisplayState> = _libraryItemDisplayState.asStateFlow()

  private val _libraryItemMarqueeEnabled = MutableStateFlow<Boolean>(false)
  val libraryItemMarqueeEnabled: Boolean get() = _libraryItemMarqueeEnabled.value
  override fun setLibraryItemMarqueeEnabled(value: Boolean) {
    _libraryItemMarqueeEnabled.value = value
  }
  override fun observeLibraryItemMarqueeEnabled(): StateFlow<Boolean> = _libraryItemMarqueeEnabled.asStateFlow()

  private val _librarySortMode = MutableStateFlow<ContentSortMode>(ContentSortMode.entries.first())
  val librarySortMode: ContentSortMode get() = _librarySortMode.value
  override fun setLibrarySortMode(value: ContentSortMode) {
    _librarySortMode.value = value
  }
  override fun observeLibrarySortMode(): StateFlow<ContentSortMode> = _librarySortMode.asStateFlow()

  private val _librarySortDirection = MutableStateFlow<SortDirection>(SortDirection.entries.first())
  val librarySortDirection: SortDirection get() = _librarySortDirection.value
  override fun setLibrarySortDirection(value: SortDirection) {
    _librarySortDirection.value = value
  }
  override fun observeLibrarySortDirection(): StateFlow<SortDirection> = _librarySortDirection.asStateFlow()

  private val _authorsSortMode = MutableStateFlow<ContentSortMode>(ContentSortMode.entries.first())
  val authorsSortMode: ContentSortMode get() = _authorsSortMode.value
  override fun setAuthorsSortMode(value: ContentSortMode) {
    _authorsSortMode.value = value
  }
  override fun observeAuthorsSortMode(): StateFlow<ContentSortMode> = _authorsSortMode.asStateFlow()

  private val _authorsSortDirection = MutableStateFlow<SortDirection>(SortDirection.entries.first())
  val authorsSortDirection: SortDirection get() = _authorsSortDirection.value
  override fun setAuthorsSortDirection(value: SortDirection) {
    _authorsSortDirection.value = value
  }
  override fun observeAuthorsSortDirection(): StateFlow<SortDirection> = _authorsSortDirection.asStateFlow()

  private val _seriesSortMode = MutableStateFlow<ContentSortMode>(ContentSortMode.entries.first())
  val seriesSortMode: ContentSortMode get() = _seriesSortMode.value
  override fun setSeriesSortMode(value: ContentSortMode) {
    _seriesSortMode.value = value
  }
  override fun observeSeriesSortMode(): StateFlow<ContentSortMode> = _seriesSortMode.asStateFlow()

  private val _seriesSortDirection = MutableStateFlow<SortDirection>(SortDirection.entries.first())
  val seriesSortDirection: SortDirection get() = _seriesSortDirection.value
  override fun setSeriesSortDirection(value: SortDirection) {
    _seriesSortDirection.value = value
  }
  override fun observeSeriesSortDirection(): StateFlow<SortDirection> = _seriesSortDirection.asStateFlow()

  private val _seriesDisplayState = MutableStateFlow<GroupDisplayState>(GroupDisplayState.entries.first())
  val seriesDisplayState: GroupDisplayState get() = _seriesDisplayState.value
  override fun setSeriesDisplayState(value: GroupDisplayState) {
    _seriesDisplayState.value = value
  }
  override fun observeSeriesDisplayState(): StateFlow<GroupDisplayState> = _seriesDisplayState.asStateFlow()

  private val _collectionsDisplayState = MutableStateFlow<GroupDisplayState>(GroupDisplayState.entries.first())
  val collectionsDisplayState: GroupDisplayState get() = _collectionsDisplayState.value
  override fun setCollectionsDisplayState(value: GroupDisplayState) {
    _collectionsDisplayState.value = value
  }
  override fun observeCollectionsDisplayState(): StateFlow<GroupDisplayState> = _collectionsDisplayState.asStateFlow()

  private val _playlistsDisplayState = MutableStateFlow<GroupDisplayState>(GroupDisplayState.entries.first())
  val playlistsDisplayState: GroupDisplayState get() = _playlistsDisplayState.value
  override fun setPlaylistsDisplayState(value: GroupDisplayState) {
    _playlistsDisplayState.value = value
  }
  override fun observePlaylistsDisplayState(): StateFlow<GroupDisplayState> = _playlistsDisplayState.asStateFlow()

  private val _showConfirmDownload = MutableStateFlow<Boolean>(false)
  val showConfirmDownload: Boolean get() = _showConfirmDownload.value
  override fun setShowConfirmDownload(value: Boolean) {
    _showConfirmDownload.value = value
  }
  override fun observeShowConfirmDownload(): StateFlow<Boolean> = _showConfirmDownload.asStateFlow()

  private val _showTimeInBook = MutableStateFlow<Boolean>(false)
  val showTimeInBook: Boolean get() = _showTimeInBook.value
  override fun setShowTimeInBook(value: Boolean) {
    _showTimeInBook.value = value
  }
  override fun observeShowTimeInBook(): StateFlow<Boolean> = _showTimeInBook.asStateFlow()
}
