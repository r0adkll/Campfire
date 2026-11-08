// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import app.campfire.audioplayer.model.AudioOutputDiagnostics

/** One line on how audio is getting to the device, and why it isn't low-power when it isn't. */
internal fun AudioOutputDiagnostics?.verdict(): String {
  if (this == null) return "Nothing playing"
  val support = hardwareSupport
  return when {
    offloaded == null -> "Nothing playing"
    offloaded == true -> "Low-power: the audio hardware is playing it"
    !offloadSettingEnabled -> "Decoded here: Low-power playback is off"
    equalizerEnabled -> "Decoded here: the equalizer is on"
    support == null -> "Decoded here"
    !support.format -> {
      val rate = sampleRate?.let { " at $it Hz" }.orEmpty()
      "Decoded here: the audio hardware can't play this format$rate"
    }
    !support.speedChange -> "Decoded here: the audio hardware can't change its speed"
    !support.gapless -> "Decoded here: the audio hardware can't play it gapless"
    else -> "Decoded here"
  }
}

/** The details behind [verdict]: what's playing and what the hardware can do with it. */
internal fun AudioOutputDiagnostics.details(): String = buildString {
  append("Format: ")
  append(
    listOfNotNull(
      format,
      sampleRate?.let { "$it Hz" },
      channelCount?.let { "$it ch" },
    ).joinToString(", ").ifEmpty { "unknown" },
  )
  hardwareSupport?.let {
    append("\nHardware: format ${it.format.mark}, speed ${it.speedChange.mark}, gapless ${it.gapless.mark}")
  }
  append("\nLow-power playback: ${if (offloadSettingEnabled) "on" else "off"}")
  append(", equalizer: ${if (equalizerEnabled) "on" else "off"}")
}

private val Boolean.mark: String get() = if (this) "yes" else "no"
