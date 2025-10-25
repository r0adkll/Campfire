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

  private var currentSessionId: LibraryItemId? = null

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    // Dumb hack to get around edge case where this can be called twice from [SessionLayoutHost]
    // during init/new session
    if (currentSessionId == itemId && chapterId != null) {
      wbark { "[$this] -!-> Current session is $currentSessionId, ignoring request" }
      return
    }

    ibark { "[$this] ~~> startSession(playImmediately=$playImmediately)" }
    mediaSessionConnector.mediaControllerFlow
      .filterNotNull()
      .take(1)
      .onEach { mediaController ->
        ibark { "$mediaController <-- starting for item, playImmediately=$playImmediately" }
        currentSessionId = itemId
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
