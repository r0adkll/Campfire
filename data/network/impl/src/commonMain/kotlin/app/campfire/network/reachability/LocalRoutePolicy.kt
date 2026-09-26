// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

/** Whether the current network may try to reach a server at all. */
internal enum class RouteVerdict {
  /** Behave normally. */
  Allow,

  /** An unfamiliar Wi-Fi/Ethernet network: allow one attempt to learn whether it's another home. */
  ProbeOnce,

  /** This network can't reach the server; make no attempts. */
  Skip,
}

/**
 * Decides whether [network] can reach a server of [locality], given the fingerprints of the
 * networks it has been reached from before ([learned]) and whether the platform is blocking
 * local network traffic for lack of a permission ([localNetworkBlocked]).
 *
 * Every uncertain case resolves to [RouteVerdict.Allow] — the cost of guessing wrong there is only
 * today's behavior (fail fast, periodic probe), whereas a wrong [RouteVerdict.Skip] would hide a
 * reachable server.
 */
internal fun routeVerdict(
  locality: ServerLocality,
  network: NetworkSnapshot,
  isSupported: Boolean,
  learned: Set<NetworkFingerprint>,
  localNetworkBlocked: Boolean = false,
): RouteVerdict {
  if (locality == ServerLocality.Public) return RouteVerdict.Allow
  // A VPN can carry traffic into the home network from anywhere
  if (network.hasVpn) return RouteVerdict.Allow
  // The OS drops LAN traffic outright without the local network permission (Android 17+), so
  // every attempt would only wait out a connect timeout
  if (locality == ServerLocality.Private && localNetworkBlocked) return RouteVerdict.Skip
  if (!isSupported) return RouteVerdict.Allow
  if (locality == ServerLocality.VpnOnly) return RouteVerdict.Skip

  // A private address is unreachable over cellular without a VPN
  if (network.isCellularOnly) return RouteVerdict.Skip
  if (network.localTransport == null) return RouteVerdict.Allow

  // Nothing learned yet (fresh install, or the list was cleared): learn rather than block
  if (learned.isEmpty()) return RouteVerdict.Allow
  val fingerprint = network.fingerprint ?: return RouteVerdict.Allow
  return if (fingerprint in learned) RouteVerdict.Allow else RouteVerdict.ProbeOnce
}
