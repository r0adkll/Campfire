package app.campfire.audioplayer.test

import app.campfire.audioplayer.PlaybackController
import app.campfire.core.model.LibraryItemId

class FakePlaybackController : PlaybackController {

  var session: PlaybackControllerSession = PlaybackControllerSession.None

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    session = PlaybackControllerSession.Started(
      itemId = itemId,
      playImmediately = playImmediately,
      chapterId = chapterId,
    )
  }

  override fun stopSession(itemId: LibraryItemId) {
    session = PlaybackControllerSession.Stopped(
      itemId = itemId,
    )
  }
}

sealed interface PlaybackControllerSession {
  data object None : PlaybackControllerSession

  data class Started(
    val itemId: LibraryItemId,
    val playImmediately: Boolean,
    val chapterId: Int?,
  ) : PlaybackControllerSession

  data class Stopped(
    val itemId: LibraryItemId,
  ) : PlaybackControllerSession
}
