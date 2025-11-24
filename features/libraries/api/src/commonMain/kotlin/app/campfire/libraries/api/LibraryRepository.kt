package app.campfire.libraries.api

import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
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
   * Observe the library items for the current selected library
   * @return a [Flow] that will emit the list of [LibraryItem] for the given [LibraryId]
   */
  fun observeLibraryItems(
    filter: LibraryItemFilter?,
    sortMode: SortMode,
    sortDirection: SortDirection,
  ): Flow<List<LibraryItem>>

  /**
   * Set a library as the currently selected one
   */
  suspend fun setCurrentLibrary(library: Library)
}
