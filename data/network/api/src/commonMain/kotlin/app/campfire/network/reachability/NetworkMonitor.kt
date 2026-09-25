// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import kotlinx.coroutines.flow.StateFlow

/**
 * The device's current default network, in more detail than connected/metered: what carries the
 * traffic and, for Wi-Fi/Ethernet, a fingerprint that tells one network from another without the
 * location permission a Wi-Fi name would need.
 */
interface NetworkMonitor {

  /** False on platforms that can't describe their network; [snapshot] is then [NetworkSnapshot.Unknown]. */
  val isSupported: Boolean

  val snapshot: StateFlow<NetworkSnapshot>
}

data class NetworkSnapshot(
  val connected: Boolean,
  val metered: Boolean,
  val transports: Set<NetworkTransport>,
  /** Present for Wi-Fi/Ethernet networks with an IPv4 address. */
  val fingerprint: NetworkFingerprint?,
  /** The DHCP search domain, e.g. `lan`, when the network provides one. */
  val domain: String?,
) {

  val hasVpn: Boolean get() = NetworkTransport.Vpn in transports

  /** Whether the only way out is a cellular connection (no Wi-Fi, Ethernet or VPN). */
  val isCellularOnly: Boolean
    get() = NetworkTransport.Cellular in transports &&
      transports.none { it == NetworkTransport.Wifi || it == NetworkTransport.Ethernet || it == NetworkTransport.Vpn }

  val localTransport: NetworkTransport?
    get() = when {
      NetworkTransport.Ethernet in transports -> NetworkTransport.Ethernet
      NetworkTransport.Wifi in transports -> NetworkTransport.Wifi
      else -> null
    }

  companion object {
    /** What unsupported platforms report: nothing is known, so nothing is restricted. */
    val Unknown = NetworkSnapshot(
      connected = true,
      metered = false,
      transports = emptySet(),
      fingerprint = null,
      domain = null,
    )

    val Disconnected = NetworkSnapshot(
      connected = false,
      metered = false,
      transports = emptySet(),
      fingerprint = null,
      domain = null,
    )
  }
}

enum class NetworkTransport {
  Wifi,
  Cellular,
  Ethernet,
  Vpn,
}

/** Identifies a local network by the device's IPv4 [subnet] (e.g. `192.168.1.0/24`) and [gateway]. */
data class NetworkFingerprint(
  val subnet: String,
  val gateway: String?,
) {
  val key: String get() = "$subnet|${gateway.orEmpty()}"
}
