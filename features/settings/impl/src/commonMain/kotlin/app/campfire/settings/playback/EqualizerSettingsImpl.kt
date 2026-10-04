// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.di.AppScope
import app.campfire.settings.api.EqualizerSettings
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<EqualizerSettings>())
@Inject
class EqualizerSettingsImpl(
  override val store: SettingsStore,
) : EqualizerSettings, AppSettings() {

  private val equalizerProfileProperty = customSetting(
    key = PREF_EQUALIZER_PROFILE,
    defaultValue = EqualizerProfile(),
    getter = { it.asEqualizerProfile() ?: EqualizerProfile() },
    setter = { profile -> profile.serialize() },
  )
  override fun setEqualizerProfile(value: EqualizerProfile) = equalizerProfileProperty.set(value)
  override fun observeEqualizerProfile(): Flow<EqualizerProfile> = equalizerProfileProperty.observe()

  private val customBandGainsProperty = customSetting(
    key = PREF_EQUALIZER_CUSTOM_GAINS,
    defaultValue = EqualizerSettings.DefaultCustomBandGains,
    getter = { it.asBandGains() ?: EqualizerSettings.DefaultCustomBandGains },
    setter = { gains -> gains.joinToString(EQUALIZER_GAINS_SEPARATOR) },
  )
  override fun setCustomBandGains(value: List<Float>) = customBandGainsProperty.set(value)
  override fun observeCustomBandGains(): Flow<List<Float>> = customBandGainsProperty.observe()
}

internal const val PREF_EQUALIZER_PROFILE = "pref_equalizer_profile"
internal const val PREF_EQUALIZER_CUSTOM_GAINS = "pref_equalizer_custom_gains"
