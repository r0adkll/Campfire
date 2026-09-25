// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

/**
 * The HTTP client's side of [ServerReachability]: it reports what each request learned about the
 * server and asks whether a request should fail without touching the network. Lives in the impl
 * module so only the network layer can fail requests or mark the server unreachable.
 *
 * [origin] is a server reduced to `scheme://host:port` (see [serverOrigin]).
 */
interface ReachabilityGate {

  /**
   * Whether a request to [origin] should fail without touching the network. False whenever the
   * server isn't known to be unreachable, the device changed networks since it was, or a probe is
   * due — in which case exactly one caller claims the probe and the rest keep failing fast.
   */
  fun shouldFailFast(origin: String): Boolean

  /** The server at [origin] answered a request. */
  fun reachable(origin: String)

  /** A request to [origin] could not establish a connection. */
  fun unreachable(origin: String)
}
