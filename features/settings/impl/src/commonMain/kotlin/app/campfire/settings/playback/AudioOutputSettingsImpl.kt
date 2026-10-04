// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.AudioOutputSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsDispatcher
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<AudioOutputSettings>())
@Inject
class AudioOutputSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
  @SettingsDispatcher override val dispatcher: CoroutineDispatcher,
) : AudioOutputSettings, AppSettings() {

  private val volumeProperty = floatSetting(PREF_OUTPUT_VOLUME, AudioOutputSettings.DEFAULT_VOLUME)
  override fun setVolume(value: Float) = volumeProperty.set(value)
  override fun observeVolume(): Flow<Float> = volumeProperty.observe()

  private val outputDeviceNameProperty = stringOrNullSetting(PREF_OUTPUT_DEVICE)
  override fun setOutputDeviceName(value: String?) = outputDeviceNameProperty.set(value)
  override fun observeOutputDeviceName(): Flow<String?> = outputDeviceNameProperty.observe()
}

internal const val PREF_OUTPUT_VOLUME = "pref_audio_output_volume"
internal const val PREF_OUTPUT_DEVICE = "pref_audio_output_device"
