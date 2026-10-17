// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the user has Campfire working offline, and why. While offline no request reaches the
 * server — API, cover art, streaming, downloads or the socket — and the app runs from downloaded
 * and cached content; leaving it fires the usual reconnect, so progress recorded meanwhile syncs.
 *
 * Working offline is either a session the user started (see [OfflineChoice]) or the standing
 * "work offline on mobile data" rule. Sessions with an end condition end on their own.
 */
interface WorkOffline {

  val state: StateFlow<OfflineState>

  /** Whether the network can tell Wi-Fi from mobile data, so the Wi-Fi based options apply. */
  val canDetectWifi: Boolean

  /** Whether to skip requests right now; reads the live settings, so it's exact between [state] updates. */
  fun isOffline(): Boolean

  fun goOffline(choice: OfflineChoice)

  /**
   * Ends any session and, if the mobile data rule is what keeps Campfire offline, sets the rule
   * aside until the device is next on Wi-Fi.
   */
  fun goOnline()

  /** Emits each time a session ends on its own: its time ran out, or Wi-Fi returned. */
  val sessionEnded: Flow<Unit>
}

/** The ways to start working offline. */
enum class OfflineChoice {
  UntilTurnedOff,
  OneHour,
  FourHours,

  /** Until 6 AM: tomorrow's, or today's when it's still before then. */
  UntilTomorrow,

  /** Until the device next joins a Wi-Fi or Ethernet network. */
  UntilWifi,
}

data class OfflineState(
  /** Why Campfire is working offline; null while online. */
  val reason: OfflineReason?,
  /** Whether the device is on Wi-Fi or Ethernet right now. */
  val onWifi: Boolean,
) {
  val isOffline: Boolean get() = reason != null

  companion object {
    val Online = OfflineState(reason = null, onWifi = false)
  }
}

sealed interface OfflineReason {
  data object UntilTurnedOff : OfflineReason

  data class Until(val endsAtMillis: Long) : OfflineReason

  /** [reconnect] when it started on Wi-Fi, so that network has to be left and joined again. */
  data class UntilWifi(val reconnect: Boolean) : OfflineReason

  /** The "work offline on mobile data" rule. */
  data object OnMobileData : OfflineReason
}
