package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.common.settings.PlaybackSettings
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@ContributesTo(UserScope::class)
interface SessionComponent {
  val playbackSessionManager: PlaybackSessionManager
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DesktopPlaybackController(
  private val playbackSettings: PlaybackSettings,
  private val fatherTime: FatherTime,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : PlaybackController {
  override val currentPlayer = MutableStateFlow<AudioPlayer?>(null)

  override fun startSession(itemId: LibraryItemId, playImmediately: Boolean) {
    applicationScope.launch {
      currentPlayer.value = VlcAudioPlayer(playbackSettings, fatherTime)

      ComponentHolder.component<SessionComponent>()
        .playbackSessionManager
        .startSession(itemId, playImmediately)
    }
  }

  override fun stopSession(itemId: LibraryItemId) {
    applicationScope.launch {
      ComponentHolder.component<SessionComponent>()
        .playbackSessionManager
        .stopSession(itemId)

      currentPlayer.value?.stop()
      currentPlayer.value = null
    }
  }
}
