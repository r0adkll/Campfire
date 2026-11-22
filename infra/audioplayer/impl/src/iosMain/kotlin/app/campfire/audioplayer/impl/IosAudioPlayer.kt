// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.OnFinishedListener
import app.campfire.audioplayer.impl.kvo.observe
import app.campfire.audioplayer.impl.mediaitem.ArtworkLoader
import app.campfire.audioplayer.impl.player.InterruptionType
import app.campfire.audioplayer.impl.player.NowPlaying
import app.campfire.audioplayer.impl.player.disable
import app.campfire.audioplayer.impl.player.enable
import app.campfire.audioplayer.impl.player.getPreviousRoute
import app.campfire.audioplayer.impl.player.hasPrivateOutput
import app.campfire.audioplayer.impl.player.interruptionShouldResume
import app.campfire.audioplayer.impl.player.isOldDeviceUnavailable
import app.campfire.audioplayer.impl.player.preferredIntervals
import app.campfire.audioplayer.impl.player.supportedPlaybackRates
import app.campfire.core.logging.Cork
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.Session
import app.campfire.settings.api.PlaybackSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPChangePlaybackRateCommandEvent
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.UIKit.UIApplication
import platform.UIKit.beginReceivingRemoteControlEvents
import platform.UIKit.endReceivingRemoteControlEvents
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * The iOS [AudioPlayer]: the shared [EngineAudioPlayer] (queue, chapter timeline, sync, sleep
 * timer) driving an [AvPlayerEngine][app.campfire.audioplayer.impl.engine.AvPlayerEngine], plus
 * the iOS system integration around it — the Now Playing entry, remote commands (lock screen,
 * Control Center, headphones, CarPlay), audio interruptions and route changes.
 *
 * Everything here runs on the main thread, as does the delegate's engine.
 */
class IosAudioPlayer(
  private val delegate: EngineAudioPlayer,
  private val settings: PlaybackSettings,
  private val artworkLoader: ArtworkLoader,
) : AudioPlayer by delegate {

  private val scope = CoroutineScope(
    SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, throwable ->
      bark(LogPriority.ERROR, throwable = throwable) { "iOS player integration failed" }
    },
  )

  private val sessionObservers = listOf(
    AVAudioSession.sharedInstance().let { session ->
      NSNotificationCenter.defaultCenter.observe(
        name = AVAudioSessionInterruptionNotification,
        nsObject = session,
      ) { _, notification -> notification?.let(::onInterruption) }
    },
    AVAudioSession.sharedInstance().let { session ->
      NSNotificationCenter.defaultCenter.observe(
        name = AVAudioSessionRouteChangeNotification,
        nsObject = session,
      ) { _, notification -> notification?.let(::onRouteChange) }
    },
  )

  /** Playback was running when an interruption (a call, Siri, an alarm) took the audio session. */
  private var resumeAfterInterruption = false

  init {
    combine(
      delegate.state,
      delegate.currentTime,
      delegate.currentDuration,
      delegate.currentMetadata,
      delegate.playbackSpeed,
    ) { state, time, duration, metadata, speed ->
      NowPlaying.update(
        title = metadata.title,
        elapsed = time,
        duration = duration,
        rate = if (state == AudioPlayer.State.Playing) speed.toDouble() else 0.0,
        defaultRate = speed.toDouble(),
      )
    }.launchIn(scope)
  }

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
    onFinished: OnFinishedListener,
  ) {
    scope.launch {
      NowPlaying.setSession(session)
      setupRemoteCommands()
      artworkLoader.load(session.libraryItem.media.coverImageUrl)?.let(NowPlaying::setArtwork)
    }
    delegate.prepare(session, playImmediately, chapterId, onFinished)
  }

  override fun release() {
    tearDown()
    delegate.release()
  }

  override fun stop() {
    tearDown()
    delegate.stop()
  }

  private fun tearDown() {
    sessionObservers.forEach(AutoCloseable::close)
    // Cancelled first, so no further Now Playing update lands after the reset below
    scope.cancel()
    dispatch_async(dispatch_get_main_queue()) {
      MPRemoteCommandCenter.sharedCommandCenter().disableAll()
      UIApplication.sharedApplication.endReceivingRemoteControlEvents()
      NowPlaying.reset()
    }
  }

  private fun onInterruption(notification: NSNotification) {
    when (InterruptionType.fromNotification(notification)) {
      InterruptionType.Began -> {
        // The system has already paused AVPlayer; record the intent so the player's own
        // play/pause toggle stays in step, and remember whether to come back
        resumeAfterInterruption = delegate.playRequested
        ibark { "Audio interruption began (wasPlaying=$resumeAfterInterruption)" }
        if (resumeAfterInterruption) delegate.pause()
      }

      InterruptionType.Ended -> {
        val shouldResume = notification.interruptionShouldResume()
        ibark { "Audio interruption ended (shouldResume=$shouldResume, wasPlaying=$resumeAfterInterruption)" }
        if (shouldResume && resumeAfterInterruption && !delegate.playRequested) {
          delegate.playPause()
        }
        resumeAfterInterruption = false
      }

      InterruptionType.Unknown -> Unit
    }
  }

  private fun onRouteChange(notification: NSNotification) {
    // Headphones unplugged, a headset or car disconnected: pause rather than continue on the speaker
    if (notification.isOldDeviceUnavailable() && notification.getPreviousRoute()?.hasPrivateOutput() == true) {
      ibark { "Private audio output went away; pausing" }
      delegate.pause()
    }
  }

  private fun setupRemoteCommands() {
    UIApplication.sharedApplication.beginReceivingRemoteControlEvents()
    val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

    commandCenter.playCommand.enable {
      if (!delegate.playRequested) delegate.playPause()
      MPRemoteCommandHandlerStatusSuccess
    }
    commandCenter.pauseCommand.enable {
      delegate.pause()
      MPRemoteCommandHandlerStatusSuccess
    }
    commandCenter.togglePlayPauseCommand.enable {
      delegate.playPause()
      MPRemoteCommandHandlerStatusSuccess
    }

    commandCenter.skipForwardCommand.enable(
      setup = { preferredIntervals(settings.forwardTimeMs) },
    ) {
      delegate.seekForward()
      MPRemoteCommandHandlerStatusSuccess
    }
    commandCenter.skipBackwardCommand.enable(
      setup = { preferredIntervals(settings.backwardTimeMs) },
    ) {
      delegate.seekBackward()
      MPRemoteCommandHandlerStatusSuccess
    }

    commandCenter.nextTrackCommand.enable {
      if (settings.remoteNextPrevSkipsChapters) delegate.skipToNext() else delegate.seekForward()
      MPRemoteCommandHandlerStatusSuccess
    }
    commandCenter.previousTrackCommand.enable {
      if (settings.remoteNextPrevSkipsChapters) delegate.skipToPrevious() else delegate.seekBackward()
      MPRemoteCommandHandlerStatusSuccess
    }

    // The scrubber is chapter-relative, like the elapsed time and duration published to Now Playing
    commandCenter.changePlaybackPositionCommand.enable { event ->
      val position = (event as? MPChangePlaybackPositionCommandEvent)?.positionTime?.seconds
      val duration = delegate.currentDuration.value
      if (position == null || duration <= Duration.ZERO) return@enable MPRemoteCommandHandlerStatusCommandFailed
      delegate.seekTo((position / duration).toFloat())
      MPRemoteCommandHandlerStatusSuccess
    }

    commandCenter.changePlaybackRateCommand.enable(
      setup = { supportedPlaybackRates(settings.playbackRates) },
    ) { event ->
      val rate = (event as? MPChangePlaybackRateCommandEvent)?.playbackRate
        ?: return@enable MPRemoteCommandHandlerStatusCommandFailed
      delegate.setPlaybackSpeed(rate)
      MPRemoteCommandHandlerStatusSuccess
    }
  }

  private fun MPRemoteCommandCenter.disableAll() {
    listOf(
      playCommand,
      pauseCommand,
      togglePlayPauseCommand,
      skipForwardCommand,
      skipBackwardCommand,
      nextTrackCommand,
      previousTrackCommand,
      changePlaybackPositionCommand,
      changePlaybackRateCommand,
    ).forEach { it.disable() }
  }

  private companion object : Cork {
    override val tag: String = "IosAudioPlayer"
  }
}
