// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.volume

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import kotlin.test.Test

class OutputGainTest {

  @Test
  fun forPosition_isUnityAtTheTopAndSilentAtTheBottom() {
    assertThat(OutputGain.forPosition(1f)).isEqualTo(1f)
    assertThat(OutputGain.forPosition(0f)).isEqualTo(0f)
  }

  @Test
  fun forPosition_tapersSoTheTopOfTheSliderDoesRealWork() {
    // A linear slider would sit at 0.5 here and sound almost unchanged; the squared taper
    // attenuates far enough to be audible
    assertThat(OutputGain.forPosition(0.5f)).isEqualTo(0.25f)
    assertThat(OutputGain.forPosition(0.75f)).isBetween(0.56f, 0.57f)
  }

  @Test
  fun forPosition_clampsOutOfRangeInput() {
    assertThat(OutputGain.forPosition(-0.5f)).isEqualTo(0f)
    assertThat(OutputGain.forPosition(1.5f)).isEqualTo(1f)
  }

  @Test
  fun compose_withNothingAttenuatingIsTheUsersOwnGain() {
    assertThat(OutputGain.compose(position = 1f, muted = false, fade = 1f)).isEqualTo(1f)
    assertThat(OutputGain.compose(position = 0.5f, muted = false, fade = 1f)).isEqualTo(0.25f)
  }

  @Test
  fun compose_muteSilencesRegardlessOfPositionOrFade() {
    assertThat(OutputGain.compose(position = 1f, muted = true, fade = 1f)).isEqualTo(0f)
    assertThat(OutputGain.compose(position = 0.3f, muted = true, fade = 0.5f)).isEqualTo(0f)
  }

  @Test
  fun compose_fadeScalesTheUsersGainRatherThanReplacingIt() {
    // The regression this whole type exists for: a sleep fade half way down must attenuate
    // relative to wherever the user left the slider, not snap to some absolute value
    val half = OutputGain.compose(position = 0.5f, muted = false, fade = 0.1f)
    assertThat(half).isBetween(0.0249f, 0.0251f)
    assertThat(half).isLessThan(OutputGain.compose(position = 0.5f, muted = false, fade = 1f))
  }

  @Test
  fun compose_movingTheSliderMidFadeChangesTheGainWithoutEndingTheFade() {
    // Dragging from 0.5 to 1.0 while a fade sits at 0.1 keeps the fade's attenuation intact
    val before = OutputGain.compose(position = 0.5f, muted = false, fade = 0.1f)
    val after = OutputGain.compose(position = 1f, muted = false, fade = 0.1f)
    assertThat(before).isEqualTo(after * 0.25f)
    assertThat(after).isBetween(0.099f, 0.101f)
  }

  @Test
  fun compose_aCompletedFadeIsSilentEvenAtFullVolume() {
    assertThat(OutputGain.compose(position = 1f, muted = false, fade = 0f)).isEqualTo(0f)
  }

  @Test
  fun compose_clampsAnOutOfRangeFade() {
    assertThat(OutputGain.compose(position = 1f, muted = false, fade = 2f)).isEqualTo(1f)
    assertThat(OutputGain.compose(position = 1f, muted = false, fade = -1f)).isEqualTo(0f)
  }
}
