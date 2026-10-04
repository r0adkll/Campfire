// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.model.LibraryItemId

/** How equalizer profiles are written to settings, shared by the app-wide and per-book profiles. */
internal fun EqualizerProfile.serialize(): String = listOf(
  if (enabled) "1" else "0",
  presetId,
  bandGainsDb.joinToString(EQUALIZER_GAINS_SEPARATOR),
  loudnessGainDb.toString(),
  bassBoost.toString(),
).joinToString(EQUALIZER_FIELD_SEPARATOR)

internal fun String.asEqualizerProfile(): EqualizerProfile? {
  val fields = split(EQUALIZER_FIELD_SEPARATOR)
  if (fields.size != 5) return null
  val (enabled, presetId, gains, loudness, bass) = fields
  return EqualizerProfile(
    enabled = enabled == "1",
    presetId = presetId.ifEmpty { return null },
    bandGainsDb = gains.asBandGains() ?: return null,
    loudnessGainDb = loudness.toFloatOrNull() ?: return null,
    bassBoost = bass.toFloatOrNull() ?: return null,
  )
}

internal fun String.asBandGains(): List<Float>? {
  val gains = split(EQUALIZER_GAINS_SEPARATOR).mapNotNull { it.toFloatOrNull() }
  return gains.takeIf { it.size == EqualizerBands.BAND_COUNT }
}

internal fun Map<LibraryItemId, EqualizerProfile>.serializeProfiles(): String =
  entries.joinToString(EQUALIZER_ENTRY_SEPARATOR) { "${it.key}$EQUALIZER_FIELD_SEPARATOR${it.value.serialize()}" }

/** Corrupt entries are dropped. */
internal fun String.asItemProfileMap(): Map<LibraryItemId, EqualizerProfile> {
  return split(EQUALIZER_ENTRY_SEPARATOR)
    .mapNotNull { entry ->
      val itemId = entry.substringBefore(EQUALIZER_FIELD_SEPARATOR)
      val profile = entry.substringAfter(EQUALIZER_FIELD_SEPARATOR, "").asEqualizerProfile()
      if (itemId.isEmpty() || profile == null) null else itemId to profile
    }
    .toMap()
}

internal const val EQUALIZER_ENTRY_SEPARATOR = "::"
internal const val EQUALIZER_FIELD_SEPARATOR = "|"
internal const val EQUALIZER_GAINS_SEPARATOR = ","
