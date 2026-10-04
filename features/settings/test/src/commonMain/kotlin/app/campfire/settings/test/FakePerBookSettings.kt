// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.test

import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.model.LibraryItemId
import app.campfire.settings.api.EqualizerSettings
import app.campfire.settings.api.PerBookSettings
import app.campfire.settings.api.PlaybackSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * An in-memory [PerBookSettings] fake over [playbackSettings] and [equalizerSettings] for the app-wide values.
 */
class FakePerBookSettings(
  private val playbackSettings: PlaybackSettings = FakePlaybackSettings(),
  private val equalizerSettings: EqualizerSettings = FakeEqualizerSettings(),
) : PerBookSettings {

  private val _itemPlaybackSpeeds = MutableStateFlow<Map<LibraryItemId, Float>>(emptyMap())
  val itemPlaybackSpeeds: Map<LibraryItemId, Float> get() = _itemPlaybackSpeeds.value
  override fun setItemPlaybackSpeeds(value: Map<LibraryItemId, Float>) {
    _itemPlaybackSpeeds.value = value
  }
  override fun observeItemPlaybackSpeeds(): StateFlow<Map<LibraryItemId, Float>> = _itemPlaybackSpeeds.asStateFlow()

  override suspend fun playbackSpeedFor(itemId: LibraryItemId?): Float =
    itemId?.let { itemPlaybackSpeeds[it] } ?: playbackSettings.observePlaybackSpeed().first()

  override fun setPlaybackSpeedFor(itemId: LibraryItemId?, speed: Float) {
    if (itemId != null && itemId in itemPlaybackSpeeds) {
      setItemPlaybackSpeeds(itemPlaybackSpeeds + (itemId to speed))
    } else {
      playbackSettings.setPlaybackSpeed(speed)
    }
  }

  private val _itemEqualizerProfiles = MutableStateFlow<Map<LibraryItemId, EqualizerProfile>>(emptyMap())
  val itemEqualizerProfiles: Map<LibraryItemId, EqualizerProfile> get() = _itemEqualizerProfiles.value
  override fun setItemEqualizerProfiles(value: Map<LibraryItemId, EqualizerProfile>) {
    _itemEqualizerProfiles.value = value
  }
  override fun observeItemEqualizerProfiles(): StateFlow<Map<LibraryItemId, EqualizerProfile>> =
    _itemEqualizerProfiles.asStateFlow()

  override suspend fun equalizerProfileFor(itemId: LibraryItemId?): EqualizerProfile =
    itemId?.let { itemEqualizerProfiles[it] } ?: equalizerSettings.observeEqualizerProfile().first()

  override fun setEqualizerProfileFor(itemId: LibraryItemId?, profile: EqualizerProfile) {
    if (itemId != null && itemId in itemEqualizerProfiles) {
      setItemEqualizerProfiles(itemEqualizerProfiles + (itemId to profile))
    } else {
      equalizerSettings.setEqualizerProfile(profile)
    }
  }
}
