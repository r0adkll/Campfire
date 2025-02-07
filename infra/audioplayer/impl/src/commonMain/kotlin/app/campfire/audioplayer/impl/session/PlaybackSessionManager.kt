package app.campfire.audioplayer.impl.session

import app.campfire.core.model.LibraryItemId

/**
 * This interface is for connecting a live [app.campfire.audioplayer.AudioPlayer] instance
 * to a [app.campfire.core.model.Session] and keep it updated as the device playback is live.
 */
interface PlaybackSessionManager {

  suspend fun startSession(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean = true,
    chapterId: Int? = null,
  )

  suspend fun stopSession(libraryItemId: LibraryItemId)
}
