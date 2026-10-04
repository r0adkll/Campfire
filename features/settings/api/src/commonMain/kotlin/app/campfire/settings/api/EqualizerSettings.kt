// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerProfile
import kotlinx.coroutines.flow.Flow

interface EqualizerSettings {

  /**
   * The app-wide equalizer profile; a book can have its own in [PerBookSettings].
   */
  fun setEqualizerProfile(value: EqualizerProfile)
  fun observeEqualizerProfile(): Flow<EqualizerProfile>

  /**
   * The band gains last used for the "Custom" preset, persisted so that switching to a
   * built-in preset and back restores the user's custom curve.
   */
  fun observeCustomBandGains(): Flow<List<Float>>
  fun setCustomBandGains(value: List<Float>)

  companion object {
    val DefaultCustomBandGains: List<Float> = List(EqualizerBands.BAND_COUNT) { 0f }
  }
}
