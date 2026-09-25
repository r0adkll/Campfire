// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

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
}

enum class Reachability {
  /** Nothing has been learned yet since the last network change or server switch. */
  Unknown,
  Reachable,
  Unreachable,
}
