// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.network.reachability.NetworkMonitor
import com.r0adkll.kimchi.annotations.ContributesTo
import dev.jordond.connectivity.Connectivity
import dev.jordond.connectivity.asProvider
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Provides

@ContributesTo(AppScope::class)
interface ConnectivityModule {

  /**
   * Connectivity is derived from each platform's [NetworkMonitor], so one system monitor serves
   * both — and no platform asks the network whether it's online (the library's desktop provider
   * did that by requesting public websites).
   */
  @SingleIn(AppScope::class)
  @Provides
  fun provideConnectivity(networkMonitor: NetworkMonitor): Connectivity {
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
}
