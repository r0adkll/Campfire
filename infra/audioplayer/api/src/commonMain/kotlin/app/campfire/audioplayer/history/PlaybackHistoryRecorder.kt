package app.campfire.audioplayer.history

import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

/**
 * Records and provides access to playback action history for library items.
 */
interface PlaybackHistoryRecorder {

  /**
   * Record a playback action.
   */
  suspend fun record(action: PlaybackAction)

  /**
   * Observe all playback actions for a given library item, ordered by most recent first.
   */
  fun observeActions(libraryItemId: LibraryItemId): Flow<List<PlaybackAction>>

  /**
   * Get all playback actions for a given library item, ordered by most recent first.
   */
  suspend fun getActions(libraryItemId: LibraryItemId): List<PlaybackAction>

  /**
   * Delete all playback history for a given library item.
   */
  suspend fun clearActions(libraryItemId: LibraryItemId)

  /**
   * Delete all playback history for the current user.
   */
  suspend fun clearAll()
}
