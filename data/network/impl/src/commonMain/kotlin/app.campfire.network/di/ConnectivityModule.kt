package app.campfire.network.di

import app.campfire.core.di.AppScope
import dev.jordond.connectivity.Connectivity
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface ConnectivityModule {

  @SingleIn(AppScope::class)
  @Provides
  fun provideConnectivity(): Connectivity = createConnectivity()
}

expect fun createConnectivity(): Connectivity
