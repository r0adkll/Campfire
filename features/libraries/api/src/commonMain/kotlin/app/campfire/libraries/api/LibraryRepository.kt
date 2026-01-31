package app.campfire.libraries.api

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
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
}
