// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.AudioOutputSettings
import app.campfire.settings.store.AppSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<AudioOutputSettings>())
@Inject
class AudioOutputSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : AudioOutputSettings, AppSettings() {

  private val volumeProperty = floatSetting(PREF_OUTPUT_VOLUME, DEFAULT_OUTPUT_VOLUME)
  override val volume: Float by volumeProperty
  override fun setVolume(value: Float) = volumeProperty.set(value)
  override fun observeVolume(): StateFlow<Float> = volumeProperty.observe()

  private val outputDeviceNameProperty = stringOrNullSetting(PREF_OUTPUT_DEVICE)
  override val outputDeviceName: String? by outputDeviceNameProperty
  override fun setOutputDeviceName(value: String?) = outputDeviceNameProperty.set(value)
  override fun observeOutputDeviceName(): StateFlow<String?> = outputDeviceNameProperty.observe()
}

internal const val PREF_OUTPUT_VOLUME = "pref_audio_output_volume"
internal const val PREF_OUTPUT_DEVICE = "pref_audio_output_device"

internal const val DEFAULT_OUTPUT_VOLUME = 1f
