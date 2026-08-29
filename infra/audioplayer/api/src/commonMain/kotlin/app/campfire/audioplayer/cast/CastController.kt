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
   * (Android 17+ local network access). The device picker surfaces an opt-in action for it —
   * the permission is never requested automatically.
   */
  val needsLocalNetworkPermission: StateFlow<Boolean> get() = MutableStateFlow(false)

  /**
   * The in-flight or failed attempt to connect to a device that [CastDevice.requiresSession],
   * or null when no attempt is pending. Cleared to null on success; a [ConnectionAttempt.Status.Failed]
   * value persists until the next attempt or until the device picker closes.
   */
  val connectionAttempt: StateFlow<ConnectionAttempt?> get() = MutableStateFlow(null)

  fun connect(device: CastDevice)

  /** Ends the active cast session, if any, returning playback to this device. */
  fun disconnect() {}

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

data class ConnectionAttempt(
  val deviceId: String,
  val status: Status,
) {
  enum class Status {
    Connecting,
    Failed,
  }
}

abstract class CastDevice(
  val id: String,
  val name: String,
  val description: String?,
  val iconUri: String?,
  val type: Type,
  val isSelected: Boolean,
  /**
   * True when connecting to this device is asynchronous (a Google Cast session must be
   * established) rather than an instant output switch (Bluetooth, built-in speaker). The
   * picker keeps itself open and shows progress for these via [CastController.connectionAttempt].
   */
  val requiresSession: Boolean = false,
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
