// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine

import app.campfire.audioplayer.engine.ffmpeg.FfmpegEngineProvider
import app.campfire.audioplayer.impl.volume.DesktopAudioOutputController
import app.campfire.settings.test.FakeAudioOutputSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The controller against the engine that actually ships, so the wiring from provider through to
 * the picker is exercised rather than faked. Enumeration is machine-dependent, so these assert the
 * shape of the answer rather than particular device names.
 */
class DesktopAudioOutputControllerTest {

  private val settings = FakeAudioOutputSettings()
  private val controller = DesktopAudioOutputController(
    settings = settings,
    engineProviders = setOf(FfmpegEngineProvider()),
  )

  @Test
  fun `device selection is supported wherever Java Sound describes real outputs`() {
    // Linux is the exception: ALSA never surfaces the Pulse/PipeWire sinks a user would recognise
    val expected = !System.getProperty("os.name").orEmpty().startsWith("Linux", ignoreCase = true)
    assertThat(controller.supportsDeviceSelection).isEqualTo(expected)
  }

  @Test
  fun `refreshing lists this machine's outputs, and never the system default itself`() {
    if (!controller.supportsDeviceSelection) return

    controller.refreshDevices()
    val devices = controller.availableDevices.value

    // A machine that can play audio at all has at least one named output beyond the synthetic
    // "Default Audio Device", which is the absence of a pin rather than a device.
    assertTrue(devices.isNotEmpty(), "expected at least one output device, got none")
    assertTrue(
      devices.none { it.name == "Default Audio Device" },
      "the synthetic default leaked into the device list: $devices",
    )
    assertTrue(devices.all { it.name.isNotBlank() }, "a device came back unnamed: $devices")
  }

  @Test
  fun `selecting a device persists it by name, and clearing it returns to the system default`() {
    if (!controller.supportsDeviceSelection) return
    controller.refreshDevices()
    val device = controller.availableDevices.value.firstOrNull() ?: return

    controller.selectDevice(device)
    assertThat(settings.outputDeviceName).isEqualTo(device.name)
    assertThat(controller.selectedDeviceName.value).isEqualTo(device.name)

    controller.selectDevice(null)
    assertThat(settings.outputDeviceName).isNull()
    assertThat(controller.selectedDeviceName.value).isNull()
  }

  @Test
  fun `a device list is only produced after a refresh, since nothing pushes changes`() {
    assertThat(controller.availableDevices.value.isEmpty()).isTrue()
    controller.refreshDevices()
    assertThat(controller.availableDevices).isNotNull()
  }
}
