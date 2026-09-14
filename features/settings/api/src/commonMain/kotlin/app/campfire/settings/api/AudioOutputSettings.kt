// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

/**
 * How the app drives its own audio output, on platforms that let it. Only desktop does today —
 * Android and iOS route volume through the system/media volume, and a second attenuator there
 * would fight the hardware keys and the media session.
 *
 * Mute is deliberately absent: it is a momentary gesture, not a preference, and an app that
 * launches into silence because of something the user did days ago reads as broken. It lives in
 * memory on [app.campfire.audioplayer.AudioOutputController] instead.
 */
interface AudioOutputSettings {

  /**
   * The volume slider's position, 0f..1f — not the gain applied to the audio. The two differ
   * because hearing is logarithmic; the mapping lives with the player. Storing the position
   * means the curve can change without migrating what users have saved.
   */
  var volume: Float
  fun observeVolume(): StateFlow<Float>

  /**
   * The name of the output device playback is pinned to, or null to follow the system default.
   *
   * A name rather than an identifier because Java Sound offers nothing else durable, and the pin
   * is kept even when that device is absent so replugging it restores the choice.
   */
  var outputDeviceName: String?
  fun observeOutputDeviceName(): StateFlow<String?>
}
