// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.volume

import app.campfire.audioplayer.AudioDevice
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class AudioDeviceResolutionTest {

  private val speakers = AudioDevice(id = "MacBook Pro Speakers", name = "MacBook Pro Speakers")
  private val headphones = AudioDevice(id = "Headphones", name = "Headphones")

  @Test
  fun `no pin follows the system default`() {
    assertThat(AudioDeviceResolution.resolve(null, listOf(speakers, headphones))).isNull()
    assertThat(AudioDeviceResolution.isPinnedMissing(null, listOf(speakers))).isFalse()
  }

  @Test
  fun `a pinned device that is present is used`() {
    assertThat(AudioDeviceResolution.resolve("Headphones", listOf(speakers, headphones)))
      .isEqualTo(headphones)
    assertThat(AudioDeviceResolution.isPinnedMissing("Headphones", listOf(speakers, headphones)))
      .isFalse()
  }

  @Test
  fun `an unplugged device falls back to the system default rather than failing`() {
    assertThat(AudioDeviceResolution.resolve("Headphones", listOf(speakers))).isNull()
    assertThat(AudioDeviceResolution.isPinnedMissing("Headphones", listOf(speakers))).isTrue()
  }

  @Test
  fun `replugging a device restores it, because the pin was never discarded`() {
    val unplugged = listOf(speakers)
    assertThat(AudioDeviceResolution.resolve("Headphones", unplugged)).isNull()

    val replugged = listOf(speakers, headphones)
    assertThat(AudioDeviceResolution.resolve("Headphones", replugged)).isEqualTo(headphones)
    assertThat(AudioDeviceResolution.isPinnedMissing("Headphones", replugged)).isFalse()
  }

  @Test
  fun `an empty device list is treated as everything being missing`() {
    assertThat(AudioDeviceResolution.resolve("Headphones", emptyList())).isNull()
    assertThat(AudioDeviceResolution.isPinnedMissing("Headphones", emptyList())).isTrue()
  }

  @Test
  fun `matching is by name, since that is the only durable handle`() {
    // Same name, different engine-side id — still the user's device
    val reIdentified = AudioDevice(id = "85", name = "Headphones")
    assertThat(AudioDeviceResolution.resolve("Headphones", listOf(reIdentified)))
      .isEqualTo(reIdentified)
  }
}
