// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.settings.api.PlaybackSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Mirrors the current [AudioPlayer] onto the system Now Playing surface and routes remote
 * commands back to it. Pure Kotlin: the platform lives behind [NowPlayingBridge].
 *
 * The system extrapolates the scrubber from the last published elapsed time and rate, so the
 * elapsed time is only re-published when metadata, duration, speed, or state change, or when the
 * actual position drifts from that extrapolation (a seek).
 */
class NowPlayingCoordinator(
  private val holder: AudioPlayerHolder,
  private val settings: PlaybackSettings,
  private val bridge: NowPlayingBridge,
  private val scope: CoroutineScope,
  private val timeSource: TimeSource = TimeSource.Monotonic,
) {

  fun start(): Job = scope.launch {
    holder.currentPlayer.collectLatest { player ->
      if (player == null) {
        bridge.setCommandHandler(null, Duration.ZERO, Duration.ZERO)
        bridge.setPlaybackState(NowPlayingState.Stopped)
        bridge.setNowPlaying(null)
      } else {
        observe(player)
      }
    }
  }

  private suspend fun observe(player: AudioPlayer) {
    bridge.setCommandHandler(
      PlayerCommands(player),
      skipForward = settings.forwardTimeMs.milliseconds,
      skipBackward = settings.backwardTimeMs.milliseconds,
    )

    var published: Published? = null

    combine(
      player.state,
      player.currentMetadata,
      player.currentDuration,
      player.playbackSpeed,
      player.currentTime,
    ) { state, metadata, duration, speed, time ->
      val session = player.preparedSession
      val rate = if (state == AudioPlayer.State.Playing) speed.toDouble() else 0.0
      NowPlayingInfo(
        title = metadata.title,
        artist = session?.libraryItem?.media?.metadata?.authorName,
        album = session?.libraryItem?.media?.metadata?.title,
        duration = duration,
        elapsed = time,
        rate = rate,
        defaultRate = speed.toDouble(),
      ) to state
    }.collect { (info, state) ->
      if (state == AudioPlayer.State.Disabled) {
        if (published != null) {
          bridge.setNowPlaying(null)
          bridge.setPlaybackState(NowPlayingState.Stopped)
          published = null
        }
        return@collect
      }

      val previous = published
      if (previous == null || previous.needsRepublish(info, timeSource)) {
        bridge.setNowPlaying(info)
        published = Published(info, timeSource.markNow())
      }
      val playbackState = state.toNowPlayingState()
      if (previous?.state != playbackState) {
        bridge.setPlaybackState(playbackState)
        published = published?.copy(state = playbackState)
      }
    }
  }

  private class Published(
    val info: NowPlayingInfo,
    val at: TimeMark,
    val state: NowPlayingState? = null,
  ) {
    fun copy(state: NowPlayingState?) = Published(info, at, state)

    fun needsRepublish(next: NowPlayingInfo, timeSource: TimeSource): Boolean {
      if (info.copy(elapsed = Duration.ZERO) != next.copy(elapsed = Duration.ZERO)) return true
      val expected = info.elapsed + at.elapsedNow() * info.rate
      return (next.elapsed - expected).absoluteValue > DRIFT_TOLERANCE
    }
  }

  private fun AudioPlayer.State.toNowPlayingState(): NowPlayingState = when (this) {
    AudioPlayer.State.Playing -> NowPlayingState.Playing
    AudioPlayer.State.Paused,
    AudioPlayer.State.Buffering,
    AudioPlayer.State.Initializing,
    -> NowPlayingState.Paused
    AudioPlayer.State.Finished,
    AudioPlayer.State.Disabled,
    -> NowPlayingState.Stopped
  }

  private class PlayerCommands(private val player: AudioPlayer) : RemoteCommandHandler {
    override fun play() {
      if (player.state.value != AudioPlayer.State.Playing) player.playPause()
    }

    override fun pause() {
      if (player.state.value == AudioPlayer.State.Playing) player.pause()
    }

    override fun togglePlayPause() = player.playPause()

    override fun skipForward() = player.seekForward()

    override fun skipBackward() = player.seekBackward()

    override fun nextTrack() = player.skipToNext()

    override fun previousTrack() = player.skipToPrevious()

    override fun seekTo(position: Duration) {
      val duration = player.currentDuration.value
      if (duration <= Duration.ZERO) return
      player.seekTo((position / duration).toFloat().coerceIn(0f, 1f))
    }
  }

  companion object {
    private val DRIFT_TOLERANCE = 1.5.seconds
  }
}
