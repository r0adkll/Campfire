package app.campfire.libraries.api

import app.campfire.core.model.Library
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {

  /**
   * Observe all libraries for the current server
   */
  fun observeAllLibraries(): Flow<List<Library>>
}
