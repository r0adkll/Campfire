// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

/**
 * The reachability layer's side of [HomeNetworks]: reading and recording the networks a server
 * has been reached from. Lives in the impl module so only the network layer can learn networks.
 *
 * [origin] is a server reduced to `scheme://host:port` (see [serverOrigin]).
 */
interface HomeNetworkLearner {

  /** Fingerprints of the networks [origin] has been reached from. */
  fun learned(origin: String): Set<NetworkFingerprint>

  /** Records that [origin] was just reached from [network]. */
  fun learn(origin: String, network: NetworkSnapshot)
}
