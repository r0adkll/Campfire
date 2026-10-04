// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface EqualizerSettings {

  /**
   * The global equalizer profile applied to items without a per-item override.
   */
  fun setEqualizerProfile(value: EqualizerProfile)
  fun observeEqualizerProfile(): Flow<EqualizerProfile>

  /**
   * The band gains last used for the "Custom" preset, persisted so that switching to a
   * built-in preset and back restores the user's custom curve.
   */
  fun observeCustomBandGains(): Flow<List<Float>>
  fun setCustomBandGains(value: List<Float>)

  /**
   * Per-item equalizer overrides, keyed by library item id. The presence of an entry means the
   * item has a per-item equalizer enabled, and its value is that item's saved profile. Items
   * without an entry use the global profile.
   */
  fun setItemEqualizerProfiles(value: Map<LibraryItemId, EqualizerProfile>)
  fun observeItemEqualizerProfiles(): Flow<Map<LibraryItemId, EqualizerProfile>>

  /**
   * The effective equalizer profile for [itemId] — its per-item override if one is enabled,
   * otherwise the global profile.
   */
  suspend fun equalizerProfileFor(itemId: LibraryItemId?): EqualizerProfile {
    return itemId?.let { observeItemEqualizerProfiles().first()[it] } ?: observeEqualizerProfile().first()
  }

  /**
   * Persist [profile] to [itemId]'s per-item override when one is enabled, otherwise to the
   * global profile.
   */
  fun setEqualizerProfileFor(itemId: LibraryItemId?, profile: EqualizerProfile)

  companion object {
    val DefaultCustomBandGains: List<Float> = List(EqualizerBands.BAND_COUNT) { 0f }
  }
}
