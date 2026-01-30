package app.campfire.libraries.api

import androidx.paging.Pager
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryItem
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
   * Observe an up-to-date pager for the provided input parameters.
   * If the user changes their library, then this will emit a new pager
   * that is keyed to that library.
   * @return a [Flow] that will emit a [Pager] of [LibraryItem]s for the given inputs and users selected library
   */
  fun observeLibraryItemPager(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<LibraryItemPager>

  /**
   * Set a library as the currently selected one
   */
  suspend fun setCurrentLibrary(library: Library)
}
