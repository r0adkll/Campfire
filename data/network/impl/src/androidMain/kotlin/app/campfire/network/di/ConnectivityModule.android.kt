// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import app.campfire.network.reachability.NetworkMonitor
import dev.jordond.connectivity.Connectivity
import dev.jordond.connectivity.asProvider
import kotlinx.coroutines.flow.map

actual fun createConnectivity(networkMonitor: NetworkMonitor): Connectivity {
  val statuses = networkMonitor.snapshot.map { snapshot ->
    if (snapshot.connected) {
      Connectivity.Status.Connected(metered = snapshot.metered)
    } else {
      Connectivity.Status.Disconnected
    }
  }
  return Connectivity(provider = statuses.asProvider()) {
    autoStart = true
  }
}
