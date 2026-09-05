// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class OutputDevicePolicyTest {

  @Test
  fun `losing bluetooth or usb output to the built-in speakers pauses playback`() {
    assertThat(OutputDevicePolicy.shouldPause(TransportType.Bluetooth, TransportType.BuiltIn, playing = true)).isTrue()
    assertThat(OutputDevicePolicy.shouldPause(TransportType.BluetoothLE, TransportType.BuiltIn, playing = true))
      .isTrue()
    assertThat(OutputDevicePolicy.shouldPause(TransportType.Usb, TransportType.BuiltIn, playing = true)).isTrue()
  }

  @Test
  fun `deliberate switches and paused playback are left alone`() {
    // Picking another device, or connecting headphones, is the user's choice
    assertThat(OutputDevicePolicy.shouldPause(TransportType.BuiltIn, TransportType.Bluetooth, playing = true)).isFalse()
    assertThat(OutputDevicePolicy.shouldPause(TransportType.Bluetooth, TransportType.Usb, playing = true)).isFalse()
    assertThat(OutputDevicePolicy.shouldPause(TransportType.AirPlay, TransportType.BuiltIn, playing = true)).isFalse()
    assertThat(OutputDevicePolicy.shouldPause(TransportType.Unknown, TransportType.BuiltIn, playing = true)).isFalse()
    // Nothing to pause
    assertThat(OutputDevicePolicy.shouldPause(TransportType.Bluetooth, TransportType.BuiltIn, playing = false))
      .isFalse()
  }

  @Test
  fun `transport codes round-trip through four character codes`() {
    assertThat(fourCC("bltn")).isEqualTo(0x626C746E)
    assertThat(TransportType.fromCode(fourCC("blue"))).isEqualTo(TransportType.Bluetooth)
    assertThat(TransportType.fromCode(fourCC("usb "))).isEqualTo(TransportType.Usb)
    assertThat(TransportType.fromCode(0x12345678)).isEqualTo(TransportType.Unknown)
  }
}
