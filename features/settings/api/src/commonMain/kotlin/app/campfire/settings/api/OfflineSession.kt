// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

/** A stretch of working offline the user started, and what ends it. */
sealed interface OfflineSession {

  /** Not working offline. */
  data object None : OfflineSession

  /** Until the user goes back online. */
  data object UntilTurnedOff : OfflineSession

  /** Until [endsAtMillis] (epoch milliseconds). */
  data class Until(val endsAtMillis: Long) : OfflineSession

  /**
   * Until the device next joins a Wi-Fi or Ethernet network. When [startedOnWifi], the network it
   * was on at the start doesn't count: it takes leaving and joining again (or another network).
   */
  data class UntilWifi(val startedOnWifi: Boolean) : OfflineSession

  companion object {

    fun fromStorageKey(key: String?): OfflineSession = when {
      key == null -> None
      key == KEY_UNTIL_TURNED_OFF -> UntilTurnedOff
      key == KEY_UNTIL_WIFI -> UntilWifi(startedOnWifi = false)
      key == KEY_UNTIL_WIFI_RECONNECT -> UntilWifi(startedOnWifi = true)
      key.startsWith(KEY_UNTIL_PREFIX) -> key.removePrefix(KEY_UNTIL_PREFIX).toLongOrNull()?.let(::Until) ?: None
      else -> None
    }

    fun OfflineSession.toStorageKey(): String? = when (this) {
      None -> null
      UntilTurnedOff -> KEY_UNTIL_TURNED_OFF
      is Until -> "$KEY_UNTIL_PREFIX$endsAtMillis"
      is UntilWifi -> if (startedOnWifi) KEY_UNTIL_WIFI_RECONNECT else KEY_UNTIL_WIFI
    }

    private const val KEY_UNTIL_TURNED_OFF = "manual"
    private const val KEY_UNTIL_WIFI = "wifi"
    private const val KEY_UNTIL_WIFI_RECONNECT = "wifi_reconnect"
    private const val KEY_UNTIL_PREFIX = "until:"
  }
}
