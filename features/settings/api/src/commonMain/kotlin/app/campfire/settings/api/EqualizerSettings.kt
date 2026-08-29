// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.StateFlow

interface EqualizerSettings {

  /**
   * The global equalizer profile applied to items without a per-item override.
   */
  var equalizerProfile: EqualizerProfile
  fun observeEqualizerProfile(): StateFlow<EqualizerProfile>

  /**
   * The band gains last used for the "Custom" preset, persisted so that switching to a
   * built-in preset and back restores the user's custom curve.
   */
  var customBandGains: List<Float>

  /**
   * Per-item equalizer overrides, keyed by library item id. The presence of an entry means the
   * item has a per-item equalizer enabled, and its value is that item's saved profile. Items
   * without an entry use the global [equalizerProfile].
   */
  var itemEqualizerProfiles: Map<LibraryItemId, EqualizerProfile>
  fun observeItemEqualizerProfiles(): StateFlow<Map<LibraryItemId, EqualizerProfile>>

  /**
   * The effective equalizer profile for [itemId] — its per-item override if one is enabled,
   * otherwise the global [equalizerProfile].
   */
  fun equalizerProfileFor(itemId: LibraryItemId?): EqualizerProfile {
    return itemId?.let { itemEqualizerProfiles[it] } ?: equalizerProfile
  }

  /**
   * Persist [profile] to [itemId]'s per-item override when one is enabled, otherwise to the
   * global [equalizerProfile].
   */
  fun setEqualizerProfileFor(itemId: LibraryItemId?, profile: EqualizerProfile) {
    if (itemId != null && itemId in itemEqualizerProfiles) {
      itemEqualizerProfiles = itemEqualizerProfiles + (itemId to profile)
    } else {
      equalizerProfile = profile
    }
  }
}
