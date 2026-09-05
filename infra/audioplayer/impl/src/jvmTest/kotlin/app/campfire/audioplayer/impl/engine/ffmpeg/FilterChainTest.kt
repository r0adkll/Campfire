// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine.ffmpeg

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.startsWith
import kotlin.test.Test

class FilterChainTest {

  private val flat = EqualizerConfig(enabled = true, preampDb = 0f, bandGainsDb = List(10) { 0f })

  @Test
  fun `normal speed without equalizer needs no graph`() {
    assertThat(FilterChain.build(1f, null, 44_100, 2)).isNull()
    assertThat(FilterChain.build(1f, flat.copy(enabled = false), 44_100, 2)).isNull()
  }

  @Test
  fun `rate changes use atempo and split above 2x`() {
    assertThat(FilterChain.build(1.5f, null, 44_100, 2))
      .isEqualTo("atempo=1.5,aformat=sample_fmts=s16:channel_layouts=stereo:sample_rates=44100")
    assertThat(FilterChain.build(3f, null, 48_000, 1))
      .isEqualTo("atempo=1.732,atempo=1.732,aformat=sample_fmts=s16:channel_layouts=mono:sample_rates=48000")
  }

  @Test
  fun `equalizer places one peaking filter per band per channel plus a preamp stage`() {
    val gains = List(10) { 0f }.toMutableList().apply { this[0] = 6f; this[9] = -3.5f }
    val chain = FilterChain.build(1f, EqualizerConfig(enabled = true, preampDb = 2f, bandGainsDb = gains), 44_100, 2)!!

    assertThat(chain).startsWith("anequalizer='c0 f=60 w=42 g=6 t=0|c0 f=170")
    assertThat(chain).contains("|c1 f=60 w=42 g=6 t=0|")
    assertThat(chain).contains("c1 f=16000 w=11200 g=-3.5 t=0'")
    assertThat(chain).contains(",volume=2dB,aformat=")
    // 10 bands x 2 channels
    assertThat(chain.split("|").size).isEqualTo(20)
  }
}
