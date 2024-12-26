package app.campfire.audioplayer

import androidx.compose.runtime.Composable
import app.campfire.core.model.LibraryItemId

/**
 * This interface is the means by which the app can acertain the [AudioPlayer] for any ongoing playback, or not
 * if none currently. Then also start a new playback session
 */
interface PlaybackController {

  /**
   * Attach this controller to a current composition so that it can register
   * its various player controller and callbacks to the lifecycle of the UI.
   *
   * This is primarily important for the Android implementation where we need to
   * register and release a [MediaController] with the UI use of it.
   */
  @Composable
  fun attachController() = Unit

  /**
   * Start a new playback session for a given library item
   */
  fun startSession(itemId: LibraryItemId, playImmediately: Boolean = true)

  /**
   * Stop a current session
   */
  fun stopSession(itemId: LibraryItemId)
}
