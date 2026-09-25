// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

interface HomeNetworkSettings {

  /**
   * Whether Campfire stops trying to reach a server with a local address (e.g. `192.168.x.x`)
   * while the device is on a network that can't reach it — cellular only, or a Wi-Fi network
   * that has never reached the server.
   * Default: `true`
   */
  var pauseAwayFromHome: Boolean
  fun observePauseAwayFromHome(): StateFlow<Boolean>

  /**
   * The networks each local server has been reached from, learned automatically. Stored on the
   * device only.
   */
  var learnedHomeNetworks: List<LearnedHomeNetwork>
  fun observeLearnedHomeNetworks(): StateFlow<List<LearnedHomeNetwork>>
}

/**
 * A network a local server was reached from. Identified by the device's IPv4 [subnet] and
 * [gateway] on that network — Android only reveals a Wi-Fi network's name with the location
 * permission, which Campfire doesn't request.
 */
data class LearnedHomeNetwork(
  /** The server this network reaches, as `scheme://host:port`. */
  val serverOrigin: String,
  /** e.g. `192.168.1.0/24` */
  val subnet: String,
  /** e.g. `192.168.1.1`, when the network advertises a default gateway. */
  val gateway: String?,
  val transport: Transport,
  /** The DHCP search domain (e.g. `lan`), when the network provides one. */
  val domain: String?,
  /** A user-given name, e.g. "Home". */
  val label: String?,
  val firstLearnedAtMs: Long,
  val lastSeenAtMs: Long,
) {

  /** Stable identity of this entry within its server. */
  val key: String get() = "$subnet|${gateway.orEmpty()}"

  enum class Transport {
    Wifi,
    Ethernet,
  }
}
