package app.campfire.libraries.api

import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {

  /**
   * Observe all libraries for the current server
   */
  fun observeAllLibraries(): Flow<List<Library>>

  /**
   * Observe the library items for a given [libraryId]
   * @param libraryId the id of the library who's items to fetch
   * @return a [Flow] that will emit the list of [LibraryItem] for the given [LibraryId]
   */
  fun observeLibraryItems(libraryId: LibraryId): Flow<List<LibraryItem>>
}
