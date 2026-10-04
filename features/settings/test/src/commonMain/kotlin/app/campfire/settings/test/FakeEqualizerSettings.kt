// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.model.LibraryItemId
import app.campfire.settings.api.EqualizerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An in-memory [EqualizerSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeEqualizerSettings : EqualizerSettings {

  private val _equalizerProfile = MutableStateFlow<EqualizerProfile>(EqualizerProfile())
  override val equalizerProfile: EqualizerProfile get() = _equalizerProfile.value
  override fun setEqualizerProfile(value: EqualizerProfile) {
    _equalizerProfile.value = value
  }
  override fun observeEqualizerProfile(): StateFlow<EqualizerProfile> = _equalizerProfile.asStateFlow()

  private val _customBandGains = MutableStateFlow<List<Float>>(List(EqualizerBands.BAND_COUNT) { 0f })
  override val customBandGains: List<Float> get() = _customBandGains.value
  override fun setCustomBandGains(value: List<Float>) {
    _customBandGains.value = value
  }
  override fun observeCustomBandGains(): StateFlow<List<Float>> = _customBandGains.asStateFlow()

  private val _itemEqualizerProfiles =
    MutableStateFlow<Map<LibraryItemId, EqualizerProfile>>(emptyMap<LibraryItemId, EqualizerProfile>())
  override val itemEqualizerProfiles: Map<LibraryItemId, EqualizerProfile> get() = _itemEqualizerProfiles.value
  override fun setItemEqualizerProfiles(value: Map<LibraryItemId, EqualizerProfile>) {
    _itemEqualizerProfiles.value = value
  }
  override fun observeItemEqualizerProfiles(): StateFlow<Map<LibraryItemId, EqualizerProfile>> =
    _itemEqualizerProfiles.asStateFlow()
}
