package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.impl.mediaitem.ArtworkLoader
import app.campfire.audioplayer.impl.player.NowPlaying
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.PlaybackSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive

@OptIn(ExperimentalForeignApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class IosPlaybackController(
  private val playbackSessionManager: PlaybackSessionManager,
  private val playbackSettings: PlaybackSettings,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val fatherTime: FatherTime,
  private val artworkLoader: ArtworkLoader,
  private val sleepTimerManagerFactory: SleepTimerManager.Factory,
  @ForScope(UserScope::class) private val userScopeHolder: CoroutineScopeHolder,
) : PlaybackController {

  override fun startSession(itemId: LibraryItemId, playImmediately: Boolean, chapterId: Int?) {
    userScopeHolder.get().launch {
      configureAudioSession()
      initializeAudioPlayerIfNeeded()
      playbackSessionManager.startSession(itemId, playImmediately, chapterId)
    }
  }

  override fun stopSession(itemId: LibraryItemId) {
    userScopeHolder.get().launch {
      disableAudioSession()
      NowPlaying.reset()
      playbackSessionManager.stopSession(itemId)
      audioPlayerHolder.release()
    }
  }

  private fun configureAudioSession() {
    try {
      AVAudioSession.sharedInstance().apply {
        setCategory(AVAudioSessionCategoryPlayback, null)
        setActive(true, null)
      }
      bark { "AVAudioSession is now configured!" }
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Error setting up iOS AudioSession" }
    }
  }

  private fun disableAudioSession() {
    try {
      AVAudioSession.sharedInstance().apply {
        setCategory(AVAudioSessionCategoryPlayback, null)
        setActive(false, null)
      }
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Error setting up iOS AudioSession" }
    }
  }

  private fun initializeAudioPlayerIfNeeded() {
    if (audioPlayerHolder.currentPlayer.value == null) {
      bark { "Initializing IosAudioPlayer..." }
      audioPlayerHolder.setCurrentPlayer(
        IosAudioPlayer(
          settings = playbackSettings,
          fatherTime = fatherTime,
          artworkLoader = artworkLoader,
          sleepTimerManagerFactory = sleepTimerManagerFactory,
        ),
      )
    }
  }
}
