// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.test

import app.campfire.audioplayer.AudioDevice
import app.campfire.audioplayer.AudioOutputController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [AudioOutputController] for tests, defaulting to full volume and unmuted so a test
 * that does not care about output gain behaves as if neither existed.
 */
class FakeAudioOutputController(
  volume: Float = 1f,
  muted: Boolean = false,
  override val isSupported: Boolean = true,
  override val supportsDeviceSelection: Boolean = false,
  devices: List<AudioDevice> = emptyList(),
) : AudioOutputController {

  private val _volume = MutableStateFlow(volume)
  override val volume: StateFlow<Float> = _volume.asStateFlow()

  private val _isMuted = MutableStateFlow(muted)
  override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  override fun setVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    _volume.value = clamped
    if (clamped > 0f) _isMuted.value = false
  }

  override fun setMuted(muted: Boolean) {
    _isMuted.value = muted
  }

  private val _availableDevices = MutableStateFlow(devices)
  override val availableDevices: StateFlow<List<AudioDevice>> = _availableDevices.asStateFlow()

  private val _selectedDeviceName = MutableStateFlow<String?>(null)
  override val selectedDeviceName: StateFlow<String?> = _selectedDeviceName.asStateFlow()

  var refreshCount = 0
    private set

  override fun selectDevice(device: AudioDevice?) {
    _selectedDeviceName.value = device?.name
  }

  override fun refreshDevices() {
    refreshCount++
  }

  /** Simulates devices appearing or disappearing between refreshes. */
  fun setDevices(devices: List<AudioDevice>) {
    _availableDevices.value = devices
  }
}
