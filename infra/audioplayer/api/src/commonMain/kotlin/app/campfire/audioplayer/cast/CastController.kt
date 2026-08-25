// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.cast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface CastController {

  val state: StateFlow<CastState>
  val availableDevices: StateFlow<List<CastDevice>>

  /**
   * True when discovering devices may require a platform permission the user hasn't granted
   * (Android 16+ local network access). The device picker surfaces an opt-in action for it —
   * the permission is never requested automatically.
   */
  val needsLocalNetworkPermission: StateFlow<Boolean> get() = MutableStateFlow(false)

  fun connect(device: CastDevice)

  /*
   * Default no-ops so implementations on platforms (or build flavors) without cast
   * support don't need to override them.
   */

  /**
   * Requests intensive device discovery while the device picker is visible. Callers must pair
   * every call with [stopActiveScan] when the picker closes — active scanning is expensive.
   */
  fun startActiveScan() {}
  fun stopActiveScan() {}

  /** User-initiated request for the permission reported by [needsLocalNetworkPermission]. */
  fun requestLocalNetworkPermission() {}
}

enum class CastState {
  Connected,
  Connecting,
  NotConnected,
  NoDevicesAvailable,
  Unavailable,
}

abstract class CastDevice(
  val id: String,
  val name: String,
  val description: String?,
  val iconUri: String?,
  val type: Type,
  val isSelected: Boolean,
) {

  override fun toString(): String {
    return "CastDevice(" +
      "id='$id', " +
      "name='$name', " +
      "description=$description, " +
      "iconUri=$iconUri, " +
      "type=$type, " +
      "isSelected=$isSelected" +
      ")"
  }

  enum class Type {
    UNKNOWN,
    TV,
    SPEAKER,
    TABLET,
    COMPUTER,
    GAME_CONSOLE,
    CAR,
    BLUETOOTH,
    SMARTPHONE,
    SMARTWATCH,
    HEADPHONES,
    USB,
    HDMI,
    HEARING_AID,
  }

  companion object {
    const val DEFAULT_ID = "DEFAULT_ROUTE"
  }
}
