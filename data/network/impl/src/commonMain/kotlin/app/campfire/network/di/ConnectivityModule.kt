// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.network.reachability.NetworkMonitor
import com.r0adkll.kimchi.annotations.ContributesTo
import dev.jordond.connectivity.Connectivity
import me.tatarka.inject.annotations.Provides

@ContributesTo(AppScope::class)
interface ConnectivityModule {

  @SingleIn(AppScope::class)
  @Provides
  fun provideConnectivity(networkMonitor: NetworkMonitor): Connectivity = createConnectivity(networkMonitor)
}

/**
 * Where the platform can describe its network ([NetworkMonitor.isSupported]), connectivity is
 * derived from [networkMonitor] so one system callback serves both; otherwise the connectivity
 * library monitors on its own.
 */
expect fun createConnectivity(networkMonitor: NetworkMonitor): Connectivity
