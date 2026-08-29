// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.audio

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test

class EqualizerPresetsTest {

  @Test
  fun `every preset has the shared band count`() {
    EqualizerPresets.all.forEach { preset ->
      assertThat(preset.bandGainsDb).hasSize(EqualizerBands.BAND_COUNT)
    }
    assertThat(EqualizerBands.centerFrequenciesHz).hasSize(EqualizerBands.BAND_COUNT)
  }

  @Test
  fun `presetIdMatching round-trips every built-in preset`() {
    EqualizerPresets.all.forEach { preset ->
      assertThat(EqualizerPresets.presetIdMatching(preset.bandGainsDb)).isEqualTo(preset.id)
    }
  }

  @Test
  fun `presetIdMatching returns custom for unrecognized gains`() {
    val custom = listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    assertThat(EqualizerPresets.presetIdMatching(custom)).isEqualTo(EqualizerPresets.CUSTOM_ID)
  }

  @Test
  fun `forId resolves built-ins and misses custom`() {
    assertThat(EqualizerPresets.forId(EqualizerPresets.WARM_ID)).isEqualTo(EqualizerPresets.Warm)
    assertThat(EqualizerPresets.forId(EqualizerPresets.CUSTOM_ID)).isEqualTo(null)
  }
}
