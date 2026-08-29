// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.model

import app.campfire.core.audio.EqualizerProfile

/**
 * The equalizer capability and configuration of an
 * [app.campfire.audioplayer.AudioPlayer].
 */
sealed interface EqualizerState {

  /**
   * The platform cannot apply an equalizer at all (e.g. iOS). The UI should hide
   * its equalizer entry point entirely.
   */
  data object Unsupported : EqualizerState

  /**
   * The platform supports an equalizer, but effects cannot be applied right now
   * (e.g. audio is rendered remotely on a Cast device). The UI should render
   * disabled.
   */
  data class Unavailable(val profile: EqualizerProfile) : EqualizerState

  /**
   * The equalizer is active and adjustable.
   */
  data class Available(val profile: EqualizerProfile) : EqualizerState
}

/**
 * The current profile carried by this state, or null when the platform has none.
 */
val EqualizerState.profileOrNull: EqualizerProfile?
  get() = when (this) {
    EqualizerState.Unsupported -> null
    is EqualizerState.Unavailable -> profile
    is EqualizerState.Available -> profile
  }
