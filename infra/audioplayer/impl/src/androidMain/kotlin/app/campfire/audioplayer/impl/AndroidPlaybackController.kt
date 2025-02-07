package app.campfire.audioplayer.impl

import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItemId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class AndroidPlaybackController(
  private val playbackSessionManager: PlaybackSessionManager,
  private val mediaSessionConnector: MediaControllerConnector,
  @ForScope(UserScope::class) private val scopeHolder: CoroutineScopeHolder,
) : PlaybackController {

  init {
    ibark { "[$this] Constructed" }
  }

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    ibark { "[$this] ~~> startSession($itemId, playImmediately=$playImmediately, chapterId=$chapterId)" }
    mediaSessionConnector.mediaControllerFlow
      .filterNotNull()
      .take(1)
      .onEach { mediaController ->
        ibark { "$mediaController <-- starting for $itemId, playImmediately=$playImmediately" }
        playbackSessionManager.startSession(itemId, playImmediately, chapterId)
      }
      .launchIn(scopeHolder.get())
  }

  override fun stopSession(itemId: LibraryItemId) {
    mediaSessionConnector.mediaControllerFlow
      .filterNotNull()
      .take(1)
      .onEach { mediaController ->
        ibark { "$this <!-- stopSession($mediaController)" }
        mediaController.stop()
        playbackSessionManager.stopSession(itemId)
      }
      .launchIn(scopeHolder.get())
  }

  companion object : Cork {
    override val tag: String = "AndroidPlaybackController"
  }
}
