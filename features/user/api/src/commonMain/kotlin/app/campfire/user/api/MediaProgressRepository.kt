package app.campfire.user.api

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import kotlinx.coroutines.flow.Flow

interface MediaProgressRepository {

  fun observeProgress(libraryItemId: LibraryItemId): Flow<MediaProgress?>
  fun observeAllProgress(): Flow<List<MediaProgress>>

  suspend fun updateProgress(
    newProgress: MediaProgress,
    force: Boolean = false,
  )

  suspend fun deleteProgress(libraryItemId: LibraryItemId)

  suspend fun markFinished(libraryItemId: LibraryItemId)

  suspend fun markNotFinished(libraryItemId: LibraryItemId)
}
