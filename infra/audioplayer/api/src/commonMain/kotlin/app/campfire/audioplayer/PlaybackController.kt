package app.campfire.audioplayer

import app.campfire.core.model.LibraryItemId

/**
 * This interface is the means by which the app can a certain the [AudioPlayer] for any ongoing playback, or not
 * if none currently. Then also start a new playback session
 */
interface PlaybackController {

  /**
   * Start a new playback session for a given library item
   */
  fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean = true,
    chapterId: Int? = null,
  )

  /**
   * Stop a current session
   */
  fun stopSession(itemId: LibraryItemId)
}
