package app.campfire.audioplayer.impl

import app.campfire.audioplayer.OneShotPlaybackController
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class AndroidOneShotPlaybackController(
  private val oneShotMediaControllerConnector: OneShotMediaControllerConnector,
  private val playbackSessionManager: PlaybackSessionManager,
) : OneShotPlaybackController {

  override suspend fun start(
    libraryItemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    // To make the notification work, we need to start the session via a [MediaController]
    // instead of the service directly.
    oneShotMediaControllerConnector.fire {
      playbackSessionManager.startSession(
        libraryItemId = libraryItemId,
        playImmediately = playImmediately,
        chapterId = chapterId,
      )
    }
  }
}
