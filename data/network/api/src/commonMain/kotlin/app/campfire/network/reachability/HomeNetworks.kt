// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import kotlinx.coroutines.flow.Flow

/** The networks a local server has been reached from, for display and editing in settings. */
interface HomeNetworks {

  fun observe(serverUrl: String): Flow<HomeNetworksState>

  fun rename(serverUrl: String, key: String, label: String?)

  fun forget(serverUrl: String, key: String)

  fun forgetAll(serverUrl: String)
}

data class HomeNetworksState(
  /** Whether the server has a local address; learned networks only matter for local servers. */
  val isLocalServer: Boolean,
  val networks: List<HomeNetwork>,
)

data class HomeNetwork(
  val key: String,
  val subnet: String,
  val gateway: String?,
  val transport: NetworkTransport,
  val domain: String?,
  val label: String?,
  val lastSeenAtMs: Long,
  /** Whether the device is on this network right now. */
  val isCurrent: Boolean,
)
