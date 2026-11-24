// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.account.api.AccountManager
import app.campfire.audioplayer.AudioOutputController
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.QueueOnStop
import app.campfire.audioplayer.impl.engine.AvPlayerEngine
import app.campfire.audioplayer.impl.mediaitem.ArtworkLoader
import app.campfire.audioplayer.impl.offline.IosOfflineDownloadManager
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.settings.api.PlaybackSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive

@OptIn(ExperimentalForeignApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class IosPlaybackController(
  private val playbackSessionManager: PlaybackSessionManager,
  private val playbackSettings: PlaybackSettings,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val artworkLoader: ArtworkLoader,
  private val sleepTimerManagerFactory: SleepTimerManager.Factory,
  private val accountManager: AccountManager,
  private val serverRequestHeaders: ServerRequestHeaders,
  private val audioOutputController: AudioOutputController,
  private val offlineDownloadManager: IosOfflineDownloadManager,
  @ForScope(UserScope::class) private val userScopeHolder: CoroutineScopeHolder,
) : PlaybackController {

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
    episodeId: PodcastEpisodeId?,
    methodOverride: PlayMethod?,
    requeueCurrent: Boolean,
  ) {
    userScopeHolder.get().launch {
      initializeAudioPlayerIfNeeded()
      playbackSessionManager.startSession(
        itemId,
        playImmediately,
        chapterId,
        episodeId,
        methodOverride,
        requeueCurrent,
      )
    }
  }

  override fun stopSession(
    itemId: LibraryItemId,
    queue: QueueOnStop,
    episodeId: PodcastEpisodeId?,
  ) {
    userScopeHolder.get().launch {
      // Stop the current item before the manager hands over to the queue: releasing afterwards
      // would tear down the player the next item was just prepared on.
      audioPlayerHolder.release()
      if (queue == QueueOnStop.PlayNext) initializeAudioPlayerIfNeeded()

      playbackSessionManager.stopSession(itemId, queue, episodeId)

      // Nothing took over, so give the audio session back to other apps
      if (audioPlayerHolder.currentPlayer.value?.preparedSession == null) {
        audioPlayerHolder.release()
        disableAudioSession()
      }
    }
  }

  /**
   * Called by the engine right before AVPlayer starts. Activating the session interrupts other
   * apps' audio, so it waits for real playback rather than happening when a session is merely
   * restored paused. It runs on every start because the system deactivates the session during an
   * interruption, and resuming afterwards needs it back.
   */
  private fun activateAudioSession() {
    try {
      AVAudioSession.sharedInstance().apply {
        setCategory(AVAudioSessionCategoryPlayback, null)
        setActive(true, null)
      }
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Error activating iOS AudioSession" }
    }
  }

  private fun disableAudioSession() {
    try {
      AVAudioSession.sharedInstance().setActive(
        false,
        withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
        error = null,
      )
    } catch (e: Exception) {
      bark(LogPriority.ERROR, throwable = e) { "Error deactivating iOS AudioSession" }
    }
  }

  private fun initializeAudioPlayerIfNeeded() {
    if (audioPlayerHolder.currentPlayer.value == null) {
      bark { "Initializing IosAudioPlayer..." }
      val enginePlayer = EngineAudioPlayer(
        settings = playbackSettings,
        // AVPlayer has no equalizer hook; the player reports EqualizerState.Unsupported
        equalizerSettings = null,
        sleepTimerManagerFactory = sleepTimerManagerFactory,
        engineFactory = { AvPlayerEngine(beforePlay = ::activateAudioSession) },
        accessTokenProvider = { userId -> accountManager.getToken(userId)?.accessToken },
        requestHeadersProvider = serverRequestHeaders::forUrl,
        audioOutputController = audioOutputController,
        offlineTrackFiles = offlineDownloadManager::localPathFor,
        // AVPlayer and its callbacks are main-thread bound
        engineDispatcher = Dispatchers.Main,
      )
      audioPlayerHolder.setCurrentPlayer(
        IosAudioPlayer(
          delegate = enginePlayer,
          settings = playbackSettings,
          artworkLoader = artworkLoader,
        ),
      )
    }
  }
}
