package app.campfire.audioplayer.history

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/**
 * Records and provides access to playback action history for library items.
 */
interface PlaybackHistoryRepository {

  /**
   * Record a playback action.
   */
  suspend fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration? = null,
  )

  /**
   * Observe all playback actions for a given library item, ordered by most recent first.
   */
  fun observe(libraryItemId: LibraryItemId): Flow<List<PlaybackAction>>

  /**
   * Get all playback actions for a given library item, ordered by most recent first.
   */
  suspend fun get(libraryItemId: LibraryItemId): List<PlaybackAction>

  /**
   * Delete all playback history for a given library item.
   */
  suspend fun clear(libraryItemId: LibraryItemId)

  /**
   * Delete all playback history for the current user.
   */
  suspend fun clearAll()
}
