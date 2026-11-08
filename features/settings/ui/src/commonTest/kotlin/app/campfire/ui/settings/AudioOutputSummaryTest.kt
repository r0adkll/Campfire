// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings

import app.campfire.audioplayer.model.AudioOutputDiagnostics
import app.campfire.audioplayer.model.AudioOutputDiagnostics.HardwareSupport
import app.campfire.ui.settings.panes.details
import app.campfire.ui.settings.panes.verdict
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AudioOutputSummaryTest {

  private val playing = AudioOutputDiagnostics(
    offloaded = false,
    format = "audio/mp4a-latm (mp4a.40.2)",
    sampleRate = 22_050,
    channelCount = 2,
    offloadSettingEnabled = true,
    equalizerEnabled = false,
    hardwareSupport = HardwareSupport(format = true, speedChange = true, gapless = true),
  )

  @Test
  fun `says when nothing is playing`() {
    assertThat((null as AudioOutputDiagnostics?).verdict()).isEqualTo("Nothing playing")
    assertThat(playing.copy(offloaded = null).verdict()).isEqualTo("Nothing playing")
  }

  @Test
  fun `offloaded is low-power`() {
    assertThat(playing.copy(offloaded = true).verdict()).isEqualTo("Low-power: the audio hardware is playing it")
  }

  @Test
  fun `decoded explains why, settings first`() {
    assertThat(playing.copy(offloadSettingEnabled = false, equalizerEnabled = true).verdict())
      .isEqualTo("Decoded here: Low-power playback is off")
    assertThat(playing.copy(equalizerEnabled = true).verdict())
      .isEqualTo("Decoded here: the equalizer is on")
    assertThat(
      playing.copy(hardwareSupport = HardwareSupport(format = false, speedChange = false, gapless = false)).verdict(),
    )
      .isEqualTo("Decoded here: the audio hardware can't play this format at 22050 Hz")
    assertThat(
      playing.copy(hardwareSupport = HardwareSupport(format = true, speedChange = false, gapless = true)).verdict(),
    )
      .isEqualTo("Decoded here: the audio hardware can't change its speed")
    assertThat(
      playing.copy(hardwareSupport = HardwareSupport(format = true, speedChange = true, gapless = false)).verdict(),
    )
      .isEqualTo("Decoded here: the audio hardware can't play it gapless")
    assertThat(playing.copy(hardwareSupport = null).verdict()).isEqualTo("Decoded here")
    assertThat(playing.verdict()).isEqualTo("Decoded here")
  }

  @Test
  fun `details list the format, hardware support and switches`() {
    assertThat(
      playing.copy(hardwareSupport = HardwareSupport(format = false, speedChange = true, gapless = true)).details(),
    )
      .isEqualTo(
        "Format: audio/mp4a-latm (mp4a.40.2), 22050 Hz, 2 ch\n" +
          "Hardware: format no, speed yes, gapless yes\n" +
          "Low-power playback: on, equalizer: off",
      )
    assertThat(playing.copy(format = null, sampleRate = null, channelCount = null, hardwareSupport = null).details())
      .isEqualTo("Format: unknown\nLow-power playback: on, equalizer: off")
  }
}
