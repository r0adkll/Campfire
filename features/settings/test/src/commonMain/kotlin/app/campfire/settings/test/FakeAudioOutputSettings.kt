// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.settings.api.AudioOutputSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [AudioOutputSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeAudioOutputSettings(volume: Float = 1f) : AudioOutputSettings {

  private val _volume = MutableStateFlow<Float>(volume)
  override val volume: Float get() = _volume.value
  override fun setVolume(value: Float) {
    _volume.value = value
  }
  override fun observeVolume(): StateFlow<Float> = _volume.asStateFlow()

  private val _outputDeviceName = MutableStateFlow<String?>(null)
  override val outputDeviceName: String? get() = _outputDeviceName.value
  override fun setOutputDeviceName(value: String?) {
    _outputDeviceName.value = value
  }
  override fun observeOutputDeviceName(): StateFlow<String?> = _outputDeviceName.asStateFlow()
}
