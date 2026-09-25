// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's shared belief about whether the signed-in user's server can be reached. The device
 * can be online with no route to the server (off the home network, server down, VPN off), so
 * this is learned from real traffic rather than platform connectivity: any response proves the
 * server reachable; a connection-level failure (refused, DNS, no route, connect timeout) marks it
 * unreachable.
 *
 * While unreachable, requests to the server fail fast instead of each waiting out a connect
 * timeout, with a periodic probe let through to notice the server's return. A network change
 * clears the belief so the new network gets a fresh attempt.
 */
interface ServerReachability {

  val status: StateFlow<Reachability>

  /** Records that the server at [serverUrl] answered (an HTTP response or socket handshake). */
  fun reportReachable(serverUrl: String)

  /**
   * Whether the current network is one the server at [serverUrl] can be reached from. False
   * only for a local server while away from home (see [Reachability.OutOfRange]); the socket
   * stays closed until it flips back.
   */
  fun observeInRange(serverUrl: String): Flow<Boolean>
}

enum class Reachability {
  /** Nothing has been learned yet since the last network change or server switch. */
  Unknown,
  Reachable,
  Unreachable,

  /**
   * A local server (e.g. `192.168.x.x`) and a network that can't reach it — cellular only, or a
   * Wi-Fi network it has never been reached from. Nothing is attempted until the network changes.
   */
  OutOfRange,
}
