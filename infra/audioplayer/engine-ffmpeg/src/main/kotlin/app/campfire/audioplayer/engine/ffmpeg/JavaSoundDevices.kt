// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine.ffmpeg

import app.campfire.audioplayer.AudioDevice
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

/**
 * The Java Sound side of device selection, kept deliberately thin: list mixers, and find one by
 * name. Every decision — which device to use, what to do when it is gone — lives above this in
 * pure code.
 *
 * **The device list can be stale and there is no way to force a refresh.** Java rebuilds its mixer
 * array only when the device *count* changes, so unplugging one device and plugging in another
 * leaves the old names in place while the native index mapping has already moved — a name can
 * therefore resolve to the wrong physical device until the count next changes. `infos` is static
 * and a fresh `ServiceLoader` reuses it, so nothing in-process can force re-enumeration; a
 * "refresh devices" affordance would be a lie. Verified experimentally, not inferred.
 */
internal object JavaSoundDevices {

  /**
   * Whether this platform's Java Sound enumeration describes the outputs a user would recognise.
   *
   * False on Linux: ALSA enumeration yields the literal `default` plus raw `hw:` cards, and never
   * the PulseAudio or PipeWire sinks that the desktop's own sound settings name, because those are
   * PCM plugins rather than cards. A picker there would list things that are not the user's
   * outputs.
   */
  val supported: Boolean
    get() = !System.getProperty("os.name").orEmpty().startsWith("Linux", ignoreCase = true)

  /**
   * Output-capable devices, excluding Java's synthetic "Default Audio Device" — that one is the
   * absence of a pin, and is reached by opening a line with no mixer at all.
   */
  fun list(): List<AudioDevice> {
    if (!supported) return emptyList()
    return runCatching {
      AudioSystem.getMixerInfo()
        .filter { it !== defaultMixerInfo() && it.supportsPlayback() }
        .map { AudioDevice(id = it.name, name = it.name) }
    }.getOrDefault(emptyList())
  }

  /**
   * Opens a line on the device named [deviceName], or on the system default when it is null or no
   * longer present.
   *
   * Resolution happens here, at open time, rather than through a held `Mixer.Info`: those compare
   * by identity and throw once the provider re-inits.
   */
  fun openLine(deviceName: String?, format: AudioFormat, bufferBytes: Int): SourceDataLine {
    val mixer = deviceName?.let { name ->
      runCatching {
        AudioSystem.getMixerInfo().firstOrNull { it.name == name && it.supportsPlayback() }
      }.getOrNull()
    }
    val line = if (mixer != null) {
      runCatching { AudioSystem.getSourceDataLine(format, mixer) }
        .getOrElse { AudioSystem.getSourceDataLine(format) }
    } else {
      // No mixer argument is the untouched original path, and the one macOS opens with
      // kAudioUnitSubType_DefaultOutput so it follows the system default while playing.
      AudioSystem.getSourceDataLine(format)
    }
    line.open(format, bufferBytes)
    return line
  }

  /** The first mixer with source lines, which is what a no-mixer request resolves to. */
  private fun defaultMixerInfo(): Mixer.Info? = runCatching {
    AudioSystem.getMixerInfo().firstOrNull { it.supportsPlayback() }
  }.getOrNull()

  private fun Mixer.Info.supportsPlayback(): Boolean = runCatching {
    AudioSystem.getMixer(this).isLineSupported(DataLine.Info(SourceDataLine::class.java, null))
  }.getOrDefault(false)
}
