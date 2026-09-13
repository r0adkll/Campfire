// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.volume

import app.campfire.audioplayer.AudioDevice

/**
 * Turns a persisted device name plus whatever is currently plugged in into a decision.
 *
 * Deliberately pure and away from the native layer: the Java Sound shim only lists mixers and
 * opens one, and everything that could be wrong — which device to use, what happens when it
 * vanishes — is decided and tested here.
 */
object AudioDeviceResolution {

  /**
   * The device to route to, or null to follow the system default.
   *
   * A pinned device that is not currently present resolves to null rather than failing: unplugging
   * headphones should keep playback going on the speakers, the way every other player behaves.
   * The *pin* survives that — see [isPinnedMissing] — so replugging restores it.
   */
  fun resolve(pinnedName: String?, available: List<AudioDevice>): AudioDevice? {
    if (pinnedName == null) return null
    return available.firstOrNull { it.name == pinnedName }
  }

  /**
   * True when a device is pinned but absent, so playback has fallen back to the system default.
   * The picker shows it as a disabled row, so the user can see both what they chose and what they
   * are actually hearing.
   */
  fun isPinnedMissing(pinnedName: String?, available: List<AudioDevice>): Boolean {
    if (pinnedName == null) return false
    return available.none { it.name == pinnedName }
  }
}
