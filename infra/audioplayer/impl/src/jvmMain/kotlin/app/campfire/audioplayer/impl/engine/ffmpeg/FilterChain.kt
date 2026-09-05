// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine.ffmpeg

import app.campfire.core.audio.EqualizerBands
import java.util.Locale
import kotlin.math.sqrt

/** Equalizer settings as the engine receives them. */
data class EqualizerConfig(
  val enabled: Boolean,
  val preampDb: Float,
  val bandGainsDb: List<Float>,
)

/**
 * Builds the libavfilter graph description for pitch-preserving rate change and the equalizer.
 *
 * - `atempo` keeps pitch and accepts 0.5..100, but degrades above 2x, so faster rates are split
 *   across two instances.
 * - `anequalizer` places one peaking filter per Campfire band at the band's own frequency, per
 *   channel, so the bands the user sees are the bands applied (no mapping onto fixed ISO bands).
 *   Each filter is as wide as the band it owns — edges at the geometric midpoints to its
 *   neighbours — so adjacent bands neither overlap (gains would stack: +6 dB everywhere measured
 *   as +13 dB) nor leave gaps. The loudness/preamp gain is a plain `volume` stage.
 * - `aformat` at the tail pins the output back to what the audio line was opened with.
 */
object FilterChain {

  /** The graph string, or null when no filtering is needed and frames can go straight to output. */
  fun build(rate: Float, equalizer: EqualizerConfig?, sampleRate: Int, channels: Int): String? {
    val stages = mutableListOf<String>()
    stages += tempoStages(rate)
    if (equalizer != null && equalizer.enabled) {
      stages += equalizerStage(equalizer, channels)
      if (equalizer.preampDb != 0f) stages += "volume=${format(equalizer.preampDb)}dB"
    }
    if (stages.isEmpty()) return null
    stages += "aformat=sample_fmts=s16:channel_layouts=${layoutName(channels)}:sample_rates=$sampleRate"
    return stages.joinToString(",")
  }

  fun layoutName(channels: Int): String = if (channels == 1) "mono" else "stereo"

  private fun tempoStages(rate: Float): List<String> {
    val tempo = rate.coerceIn(MIN_TEMPO, MAX_TEMPO)
    if (tempo == 1f) return emptyList()
    if (tempo <= SINGLE_STAGE_MAX_TEMPO) return listOf("atempo=${format(tempo)}")
    val half = sqrt(tempo.toDouble()).toFloat()
    return listOf("atempo=${format(half)}", "atempo=${format(half)}")
  }

  private fun equalizerStage(equalizer: EqualizerConfig, channels: Int): String {
    val frequencies = EqualizerBands.centerFrequenciesHz
    val params = (0 until channels).flatMap { channel ->
      frequencies.indices.map { index ->
        val gainDb = equalizer.bandGainsDb.getOrElse(index) { 0f }
        // t=0 selects the Butterworth response
        "c$channel f=${frequencies[index]} w=${bandWidthHz(index)} g=${format(gainDb)} t=0"
      }
    }
    return "anequalizer='${params.joinToString("|")}'"
  }

  /** Width of band [index] in Hz: from the geometric midpoint below to the one above. */
  internal fun bandWidthHz(index: Int): Int {
    val frequencies = EqualizerBands.centerFrequenciesHz
    val center = frequencies[index].toDouble()
    val lower = if (index == 0) {
      center / sqrt(frequencies[1] / center)
    } else {
      sqrt(frequencies[index - 1] * center)
    }
    val upper = if (index == frequencies.lastIndex) {
      minOf(NYQUIST_CEILING_HZ, center * sqrt(center / frequencies[index - 1]))
    } else {
      sqrt(center * frequencies[index + 1])
    }
    return (upper - lower).toInt()
  }

  private fun format(value: Float): String = String.format(Locale.ROOT, "%.3f", value).trimEnd('0').trimEnd('.')

  private const val MIN_TEMPO = 0.5f
  private const val MAX_TEMPO = 4f
  private const val SINGLE_STAGE_MAX_TEMPO = 2f
  private const val NYQUIST_CEILING_HZ = 20_000.0
}
