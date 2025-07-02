package app.campfire.audioplayer

import app.campfire.core.model.LibraryItemId

interface OneShotPlaybackController {

  /**
   * In-order to cold start playback on an item from outside the main app UI we need
   * to spin up a [android.media.session.MediaController] connection to the service so
   * that our system notification is properly generated. This interface will spool up a controller,
   * start playback, then release said controller.
   */
  suspend fun start(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean = true,
    chapterId: Int? = null,
  )
}
