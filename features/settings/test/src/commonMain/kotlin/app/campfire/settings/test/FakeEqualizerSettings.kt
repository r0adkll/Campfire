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
 * A simple in-memory [EqualizerSettings] fake backed by [MutableStateFlow]s for use in tests.
 */
class FakeEqualizerSettings : EqualizerSettings {

  private val _equalizerProfile = MutableStateFlow(EqualizerProfile())
  override var equalizerProfile: EqualizerProfile
    get() = _equalizerProfile.value
    set(value) { _equalizerProfile.value = value }
  override fun observeEqualizerProfile(): StateFlow<EqualizerProfile> = _equalizerProfile.asStateFlow()

  override var customBandGains: List<Float> = List(EqualizerBands.BAND_COUNT) { 0f }

  private val _itemEqualizerProfiles = MutableStateFlow(emptyMap<LibraryItemId, EqualizerProfile>())
  override var itemEqualizerProfiles: Map<LibraryItemId, EqualizerProfile>
    get() = _itemEqualizerProfiles.value
    set(value) { _itemEqualizerProfiles.value = value }
  override fun observeItemEqualizerProfiles(): StateFlow<Map<LibraryItemId, EqualizerProfile>> =
    _itemEqualizerProfiles.asStateFlow()
}
