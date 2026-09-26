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
 * timeout, with real attempts let through at growing intervals (30s up to 10m) to notice the
 * server's return. Joining a network — even rejoining the same one — starts fresh.
 */
interface ServerReachability {

  val status: StateFlow<Reachability>

  /** Records that the server at [serverUrl] answered (an HTTP response or socket handshake). */
  fun reportReachable(serverUrl: String)

  /**
   * Whether the current network can reach the server at [serverUrl] at all. False only when it
   * certainly can't (see [Reachability.OutOfRange]); the socket stays closed until it flips back.
   */
  fun observeInRange(serverUrl: String): Flow<Boolean>

  /**
   * Whether [serverUrl] has a local address (e.g. `192.168.x.x`, `*.local`, Tailscale), judged
   * from the address alone — the servers the mobile-data and local-network rules apply to.
   */
  fun isLocalServer(serverUrl: String): Boolean
}

enum class Reachability {
  /** Nothing has been learned yet since the last network change or server switch. */
  Unknown,
  Reachable,
  Unreachable,

  /**
   * A local server (e.g. `192.168.x.x`) and a network that can't reach it — mobile data only, or
   * Android's local network permission missing. Nothing is attempted until that changes.
   */
  OutOfRange,
}
