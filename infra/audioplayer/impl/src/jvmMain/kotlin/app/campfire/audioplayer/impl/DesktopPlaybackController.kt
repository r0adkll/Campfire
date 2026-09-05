// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.account.api.AccountManager
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.impl.engine.VlcPlaybackEngine
import app.campfire.audioplayer.impl.session.PlaybackSessionManager
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.settings.api.EqualizerSettings
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
  private val equalizerSettings: EqualizerSettings,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val sleepTimerManagerFactory: SleepTimerManager.Factory,
  private val accountManager: AccountManager,
  @ForScope(UserScope::class) private val userScopeHolder: CoroutineScopeHolder,
) : PlaybackController {

  override fun startSession(
    itemId: LibraryItemId,
    playImmediately: Boolean,
    chapterId: Int?,
    episodeId: PodcastEpisodeId?,
    methodOverride: PlayMethod?,
  ) {
    userScopeHolder.get().launch {
      initializeAudioPlayerIfNeeded()
      playbackSessionManager.startSession(itemId, playImmediately, chapterId, episodeId, methodOverride)
    }
  }

  override fun stopSession(
    itemId: LibraryItemId,
    clearQueue: Boolean,
    episodeId: PodcastEpisodeId?,
  ) {
    userScopeHolder.get().launch {
      playbackSessionManager.stopSession(itemId, clearQueue, episodeId)
      audioPlayerHolder.release()
    }
  }

  private fun initializeAudioPlayerIfNeeded() {
    if (audioPlayerHolder.currentPlayer.value == null) {
      // Constructing the player is cheap; the native engine is created lazily on its own thread
      // the first time a session is prepared.
      audioPlayerHolder.setCurrentPlayer(
        DesktopAudioPlayer(
          settings = playbackSettings,
          equalizerSettings = equalizerSettings,
          sleepTimerManagerFactory = sleepTimerManagerFactory,
          engineFactory = VlcPlaybackEngine.Factory(),
          accessTokenProvider = { userId -> accountManager.getToken(userId)?.accessToken },
        ),
      )
    }
  }
}
