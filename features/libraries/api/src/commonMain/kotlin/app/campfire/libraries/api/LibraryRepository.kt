package app.campfire.libraries.api

import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {

  /**
   * Observe the current selected library
   */
  fun observeCurrentLibrary(): Flow<Library>

  /**
   * Observe all libraries for the current server
   */
  fun observeAllLibraries(): Flow<List<Library>>

  /**
   * Observe the library items for the current selected library
   * @return a [Flow] that will emit the list of [LibraryItem] for the given [LibraryId]
   */
  fun observeLibraryItems(): Flow<List<LibraryItem>>

  /**
   * Set a library as the currently selected one
   */
  suspend fun setCurrentLibrary(library: Library)
}
