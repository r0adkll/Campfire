package app.campfire.audioplayer

import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.StateFlow

/**
 * This interface is the means by which the app can acertain the [AudioPlayer] for any ongoing playback, or not
 * if none currently. Then also start a new playback session
 */
interface PlaybackController {

  /**
   * Access the current audio player
   */
  val currentPlayer: StateFlow<AudioPlayer?>

  /**
   * Start a new playback session for a given library item
   */
  fun startSession(itemId: LibraryItemId, playImmediately: Boolean = true)

  /**
   * Stop a current session
   */
  fun stopSession(itemId: LibraryItemId)
}
