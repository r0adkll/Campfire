// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.model

/**
 * How the local player is getting audio to the device, for field debugging: whether the device's
 * audio hardware plays it directly ("offload") or it's decoded here, and why.
 */
data class AudioOutputDiagnostics(
  /** True while playing to the hardware directly, false while decoding here; null with no output. */
  val offloaded: Boolean?,
  /** The audio being played, such as "audio/mp4a-latm (mp4a.40.2)", or null before any. */
  val format: String?,
  val sampleRate: Int?,
  val channelCount: Int?,
  /** Whether Low-power playback is turned on in settings. */
  val offloadSettingEnabled: Boolean,
  /** Whether the equalizer is on, which needs decoded audio. */
  val equalizerEnabled: Boolean,
  /** What the hardware can do with [format]; null before any audio is played. */
  val hardwareSupport: HardwareSupport?,
) {

  data class HardwareSupport(
    val format: Boolean,
    val speedChange: Boolean,
    val gapless: Boolean,
  )
}
