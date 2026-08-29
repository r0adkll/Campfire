// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.audio

/**
 * The fixed band layout shared by every equalizer engine. The center frequencies match
 * VLC's 10 ISO bands so the desktop (libvlc) equalizer maps index-for-index and the
 * Android engine builds its bands from the same list.
 */
object EqualizerBands {
  val centerFrequenciesHz: List<Int> = listOf(60, 170, 310, 600, 1_000, 3_000, 6_000, 12_000, 14_000, 16_000)

  const val BAND_COUNT = 10

  val BandGainRangeDb = -12f..12f
  val LoudnessGainRangeDb = 0f..15f
  val BassBoostRange = 0f..1f
}

/**
 * A complete, user-adjustable equalizer configuration.
 *
 * @param enabled Master switch; when false no audio processing is applied.
 * @param presetId One of [EqualizerPresets] ids, or [EqualizerPresets.CUSTOM_ID].
 * @param bandGainsDb Gain in dB for each of the [EqualizerBands.BAND_COUNT] bands.
 * @param loudnessGainDb Extra loudness gain in dB, within [EqualizerBands.LoudnessGainRangeDb].
 * @param bassBoost Normalized bass boost strength, within [EqualizerBands.BassBoostRange].
 */
data class EqualizerProfile(
  val enabled: Boolean = false,
  val presetId: String = EqualizerPresets.FLAT_ID,
  val bandGainsDb: List<Float> = List(EqualizerBands.BAND_COUNT) { 0f },
  val loudnessGainDb: Float = 0f,
  val bassBoost: Float = 0f,
)

data class EqualizerPreset(
  val id: String,
  val bandGainsDb: List<Float>,
)

/**
 * The built-in presets. Display names are localized in the UI layer keyed off the stable
 * preset id; no user-facing text lives here.
 */
object EqualizerPresets {
  const val FLAT_ID = "flat"
  const val VOICE_BOOST_ID = "voice_boost"
  const val PODCAST_ID = "podcast"
  const val BASS_BOOST_ID = "bass_boost"
  const val TREBLE_BOOST_ID = "treble_boost"
  const val WARM_ID = "warm"
  const val REDUCE_NOISE_ID = "reduce_noise"
  const val CUSTOM_ID = "custom"

  val Flat = EqualizerPreset(
    id = FLAT_ID,
    bandGainsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
  )

  val VoiceBoost = EqualizerPreset(
    id = VOICE_BOOST_ID,
    bandGainsDb = listOf(-4f, -3f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f),
  )

  /**
   * A fuller voice curve: where [VoiceBoost] cuts the lows for crispness, this keeps the
   * low-mid body that gives podcast-style speech its warmth.
   */
  val Podcast = EqualizerPreset(
    id = PODCAST_ID,
    bandGainsDb = listOf(3f, 4f, 5f, 5f, 4f, 3f, 2f, 1f, 0f, 0f),
  )

  val BassBoost = EqualizerPreset(
    id = BASS_BOOST_ID,
    bandGainsDb = listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
  )

  val TrebleBoost = EqualizerPreset(
    id = TREBLE_BOOST_ID,
    bandGainsDb = listOf(0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 5f, 5f),
  )

  val Warm = EqualizerPreset(
    id = WARM_ID,
    bandGainsDb = listOf(3f, 2f, 1f, 0f, 0f, -1f, -2f, -2f, -3f, -3f),
  )

  /**
   * Tames both extremes — rumble at the bottom, hiss at the top — for older or
   * poorly-mastered recordings.
   */
  val ReduceNoise = EqualizerPreset(
    id = REDUCE_NOISE_ID,
    bandGainsDb = listOf(-3f, -2f, -1f, 0f, 0f, 0f, -1f, -2f, -3f, -3f),
  )

  /**
   * All built-in presets in the order they should appear in the preset selector.
   */
  val all: List<EqualizerPreset> = listOf(Flat, VoiceBoost, Podcast, BassBoost, TrebleBoost, Warm, ReduceNoise)

  fun forId(id: String): EqualizerPreset? = all.find { it.id == id }

  /**
   * Returns the id of the built-in preset whose gains match [gains],
   * or [CUSTOM_ID] when none do.
   */
  fun presetIdMatching(gains: List<Float>): String {
    return all.find { it.bandGainsDb == gains }?.id ?: CUSTOM_ID
  }
}
