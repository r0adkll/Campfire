// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import app.campfire.core.logging.bark

/**
 * Lets the device's audio hardware decode and play supported audio ("offload"), so the CPU can sleep
 * while a book plays instead of decoding it.
 *
 * Offload is only used where the hardware can also change playback speed and play gapless, so
 * neither stops working, and never while the equalizer is on: its effects work on decoded audio,
 * so they can't process an offloaded stream.
 *
 * Switching to decoded audio mid-playback needs care. Effects created on an offloaded session fail,
 * and the decoded output that replaces it can stall (Media3 then reports the player stuck). So when
 * leaving offload this flushes the pipeline with a seek in place, and the equalizer's effects wait,
 * via [runWhenDecoded], until a decoded output is playing.
 */
@OptIn(UnstableApi::class)
internal class AudioOffloadController(
  private val exoPlayer: ExoPlayer,
) {

  private var settingEnabled = true
  private var equalizerEnabled = false

  /** The audio track the player is playing to, if any. */
  private var audioTrack: AudioSink.AudioTrackConfig? = null
  private var pendingUntilDecoded: (() -> Unit)? = null

  init {
    exoPlayer.addAnalyticsListener(
      object : AnalyticsListener {
        override fun onAudioTrackInitialized(
          eventTime: AnalyticsListener.EventTime,
          audioTrackConfig: AudioSink.AudioTrackConfig,
        ) {
          audioTrack = audioTrackConfig
          bark { "Audio track initialized (offload=${audioTrackConfig.offload})" }
          if (!audioTrackConfig.offload) {
            pendingUntilDecoded?.let { block ->
              pendingUntilDecoded = null
              block()
            }
          }
        }

        override fun onAudioTrackReleased(
          eventTime: AnalyticsListener.EventTime,
          audioTrackConfig: AudioSink.AudioTrackConfig,
        ) {
          // A replacement can be initialized before the old track is released
          if (audioTrack == audioTrackConfig) audioTrack = null
        }
      },
    )
    applyPreferences()
  }

  private val isPlayingOffloaded: Boolean get() = audioTrack?.offload == true

  fun setSettingEnabled(enabled: Boolean) {
    settingEnabled = enabled
    applyPreferences()
  }

  fun setEqualizerEnabled(enabled: Boolean) {
    equalizerEnabled = enabled
    applyPreferences()
  }

  /**
   * Runs [block] now if the audio is decoded here (or nothing is playing), otherwise once the
   * offloaded output has been replaced by a decoded one. Only the latest block is kept.
   */
  fun runWhenDecoded(block: () -> Unit) {
    if (isPlayingOffloaded) {
      pendingUntilDecoded = block
    } else {
      pendingUntilDecoded = null
      block()
    }
  }

  private fun applyPreferences() {
    val preferences = audioOffloadPreferences(enabled = settingEnabled && !equalizerEnabled)
    val current = exoPlayer.trackSelectionParameters
    if (current.audioOffloadPreferences == preferences) return

    exoPlayer.trackSelectionParameters = current.buildUpon()
      .setAudioOffloadPreferences(preferences)
      .build()

    val leavingOffload = preferences.audioOffloadMode == AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
    if (leavingOffload && isPlayingOffloaded && exoPlayer.playbackState != Player.STATE_IDLE) {
      // A seek in place flushes the renderer and audio sink, so the decoded output starts clean
      // instead of stalling behind the offloaded track it replaces
      exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, exoPlayer.currentPosition)
    }
  }

  companion object {

    /** Offload where the hardware can keep speed changes and gapless playback working. */
    fun audioOffloadPreferences(enabled: Boolean): AudioOffloadPreferences = if (enabled) {
      AudioOffloadPreferences.Builder()
        .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
        .setIsSpeedChangeSupportRequired(true)
        .setIsGaplessSupportRequired(true)
        .build()
    } else {
      AudioOffloadPreferences.DEFAULT
    }
  }
}
