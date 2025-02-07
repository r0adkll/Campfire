package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.PlaybackSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DesktopPlaybackController(
  private val playbackSessionManager: PlaybackSessionManager,
  private val playbackSettings: PlaybackSettings,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val fatherTime: FatherTime,
  private val sleepTimerManagerFactory: SleepTimerManager.Factory,
  @ForScope(UserScope::class) private val userScopeHolder: CoroutineScopeHolder,
) : PlaybackController {

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
  ) {
    userScopeHolder.get().launch {
      initializeAudioPlayerIfNeeded()
      playbackSessionManager.startSession(itemId, playImmediately, chapterId)
    }
  }

  override fun stopSession(itemId: LibraryItemId) {
    userScopeHolder.get().launch {
      playbackSessionManager.stopSession(itemId)
      audioPlayerHolder.release()
    }
  }

  private fun initializeAudioPlayerIfNeeded() {
    if (audioPlayerHolder.currentPlayer.value == null) {
      audioPlayerHolder.setCurrentPlayer(VlcAudioPlayer(playbackSettings, fatherTime, sleepTimerManagerFactory))
    }
  }
}
