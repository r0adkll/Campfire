// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.AudioOutputSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, boundType = AudioOutputSettings::class)
@Inject
class AudioOutputSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : AudioOutputSettings, AppSettings() {

  private val volumeProperty = floatSetting(PREF_OUTPUT_VOLUME, DEFAULT_OUTPUT_VOLUME)
  override var volume: Float by volumeProperty
  override fun observeVolume(): StateFlow<Float> = volumeProperty.observe()

  private val outputDeviceNameProperty = stringOrNullSetting(PREF_OUTPUT_DEVICE)
  override var outputDeviceName: String? by outputDeviceNameProperty
  override fun observeOutputDeviceName(): StateFlow<String?> = outputDeviceNameProperty.observe()
}

internal const val PREF_OUTPUT_VOLUME = "pref_audio_output_volume"
internal const val PREF_OUTPUT_DEVICE = "pref_audio_output_device"

internal const val DEFAULT_OUTPUT_VOLUME = 1f
