// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine.ffmpeg

import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerPresets
import assertk.assertThat
import assertk.assertions.isLessThan
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.test.Test
import org.bytedeco.javacpp.Loader

/**
 * Measures what the equalizer graph does to loudness by running the bundled `ffmpeg` binary over
 * pink noise (speech-like spectrum) with `volumedetect`. A flat equalizer must be transparent,
 * and every preset must land where FFmpeg's own `equalizer` cascade with the same gains and band
 * edges lands — the regression a listener reports as "the EQ makes it quiet" or "too loud".
 */
class FilterChainLoudnessTest {

  private val ffmpeg: String by lazy { Loader.load(org.bytedeco.ffmpeg.ffmpeg::class.java) }

  @Test
  fun `a flat equalizer leaves loudness unchanged`() {
    val reference = meanVolumeDb(chain = null)
    val flat = meanVolumeDb(chain(List(10) { 0f }))
    println("reference=$reference dB flat=$flat dB")
    assertThat(abs(flat - reference)).isLessThan(0.5)
  }

  @Test
  fun `every preset changes loudness like a reference equalizer cascade with the same gains`() {
    val baseline = meanVolumeDb(chain = null)
    EqualizerPresets.all.forEach { preset ->
      val ours = meanVolumeDb(chain(preset.bandGainsDb)) - baseline
      val reference = meanVolumeDb(referenceCascade(preset.bandGainsDb)) - baseline
      println("${preset.id}: ours=${"%+.1f".format(ours)} dB reference=${"%+.1f".format(reference)} dB")
      assertThat(abs(ours - reference), preset.id).isLessThan(1.5)
    }
  }

  private fun chain(gains: List<Float>) = FilterChain.build(1f, EqualizerConfig(true, 0f, gains), 44_100, 2)!!

  /** One `equalizer` peaking stage per band with the same centres, widths, and gains. */
  private fun referenceCascade(gains: List<Float>): String {
    return EqualizerBands.centerFrequenciesHz.indices.joinToString(",") { index ->
      "equalizer=f=${EqualizerBands.centerFrequenciesHz[index]}:width_type=h:" +
        "width=${FilterChain.bandWidthHz(index)}:g=${gains[index]}"
    }
  }

  private fun meanVolumeDb(chain: String?): Double {
    val filters = listOfNotNull("aformat=channel_layouts=stereo", chain, "volumedetect").joinToString(",")
    val process = ProcessBuilder(
      ffmpeg,
      "-hide_banner",
      "-f", "lavfi",
      "-i", "anoisesrc=color=pink:amplitude=0.3:sample_rate=44100:duration=3",
      "-af", filters,
      "-f", "null", "-",
    ).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor(60, TimeUnit.SECONDS)) { "ffmpeg did not finish" }
    check(process.exitValue() == 0) { "ffmpeg failed:\n$output" }
    val match = Regex("mean_volume: (-?[0-9.]+) dB").find(output) ?: error("no volumedetect output:\n$output")
    return match.groupValues[1].toDouble()
  }
}
