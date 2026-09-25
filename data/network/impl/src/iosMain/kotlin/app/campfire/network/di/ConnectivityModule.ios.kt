// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import app.campfire.network.reachability.NetworkMonitor
import dev.jordond.connectivity.Connectivity

@Suppress("UNUSED_PARAMETER")
actual fun createConnectivity(networkMonitor: NetworkMonitor): Connectivity {
  return Connectivity {
    autoStart = true
  }
}
