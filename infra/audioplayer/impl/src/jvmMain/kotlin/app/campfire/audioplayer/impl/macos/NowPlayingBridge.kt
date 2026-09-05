// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import kotlin.time.Duration

/**
 * The system "Now Playing" surface: metadata and transport state shown in Control Center and on
 * connected accessories, plus the remote commands (media keys, AirPods taps, headset buttons)
 * routed back to the app. Abstracted so [NowPlayingCoordinator] is testable without macOS.
 */
interface NowPlayingBridge {

  /** Publish [info], or clear the system's entry when null. */
  fun setNowPlaying(info: NowPlayingInfo?)

  fun setPlaybackState(state: NowPlayingState)

  /**
   * Route remote commands to [handler], or stop handling them when null. Skip intervals are
   * advertised so the system draws the matching skip buttons.
   */
  fun setCommandHandler(handler: RemoteCommandHandler?, skipForward: Duration, skipBackward: Duration)
}

data class NowPlayingInfo(
  val title: String?,
  val artist: String?,
  val album: String?,
  val duration: Duration,
  val elapsed: Duration,
  /** Current rate; 0 while paused. */
  val rate: Double,
  /** The rate playback resumes at. */
  val defaultRate: Double,
)

enum class NowPlayingState { Playing, Paused, Stopped }

interface RemoteCommandHandler {
  fun play()
  fun pause()
  fun togglePlayPause()
  fun skipForward()
  fun skipBackward()
  fun nextTrack()
  fun previousTrack()

  /** Scrubber position within the currently advertised item, i.e. relative to [NowPlayingInfo.duration]. */
  fun seekTo(position: Duration)
}
