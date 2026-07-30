// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.libraries.api.paging.LibraryItemPager
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {

  /**
   * Observe the current selected library
   */
  fun observeCurrentLibrary(refresh: Boolean = true): Flow<Library>

  /**
   * Observe all libraries for the current server
   */
  fun observeAllLibraries(refresh: Boolean = true): Flow<List<Library>>

  /**
   * Observe every library item in the currently selected library as a flat list, sorted by
   * title. Refreshes from the network when [refresh] is true.
   *
   * Intended for whole-library consumers such as the Android Auto "Shows" tab for podcast
   * libraries — it materializes the entire library in memory, so prefer
   * [createLibraryItemPager] for UI listing of potentially large book libraries.
   */
  fun observeCurrentLibraryItems(refresh: Boolean = true): Flow<List<LibraryItem>>

  fun createLibraryItemPager(
    user: User,
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): LibraryItemPager

  fun observeFilteredLibraryCount(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?>

  /**
   * Set a library as the currently selected one
   */
  suspend fun setCurrentLibrary(library: Library)

  /**
   * One-shot fetch of the library state required by the "Add podcast" flow: the list of folders
   * configured on the library and its iTunes search region setting. Pulls from the server (does
   * not consult cache) so freshly-edited library settings are reflected immediately.
   */
  suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext>
}
