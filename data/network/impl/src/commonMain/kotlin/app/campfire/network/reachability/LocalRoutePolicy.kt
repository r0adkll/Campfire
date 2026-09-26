// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

/** Whether the current network may try to reach a server at all. */
internal enum class RouteVerdict {
  /** Behave normally (failures still back off). */
  Allow,

  /** This network can't reach the server; make no attempts. */
  Skip,
}

/**
 * Decides whether [network] can reach a server of [locality] at all. Only cases that are certain
 * resolve to [RouteVerdict.Skip]; everything else is left to real attempts and their backoff,
 * since a network's addresses can't reliably tell home from elsewhere.
 *
 * @param isSupported whether the platform can describe its network; nothing network-based is
 * decided where it can't
 * @param avoidMobileData the user's choice to skip a local server over mobile data
 * @param localNetworkBlocked whether the platform is dropping local network traffic for lack of a
 * permission (Android 17+)
 */
internal fun routeVerdict(
  locality: ServerLocality,
  network: NetworkSnapshot,
  isSupported: Boolean,
  avoidMobileData: Boolean,
  localNetworkBlocked: Boolean = false,
): RouteVerdict {
  if (locality == ServerLocality.Public) return RouteVerdict.Allow
  // A VPN can carry traffic into the home network from anywhere
  if (network.hasVpn) return RouteVerdict.Allow
  // The OS drops LAN traffic outright without the local network permission, so every attempt
  // would only wait out a connect timeout
  if (locality == ServerLocality.Private && localNetworkBlocked) return RouteVerdict.Skip
  if (!isSupported || !avoidMobileData) return RouteVerdict.Allow

  return when {
    // Only reachable through the VPN, which isn't up
    locality == ServerLocality.VpnOnly -> RouteVerdict.Skip
    // A private address is unreachable over mobile data without a VPN
    network.isCellularOnly -> RouteVerdict.Skip
    else -> RouteVerdict.Allow
  }
}
