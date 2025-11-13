package app.campfire.network.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesTo
import dev.jordond.connectivity.Connectivity
import me.tatarka.inject.annotations.Provides

@ContributesTo(AppScope::class)
interface ConnectivityModule {

  @SingleIn(AppScope::class)
  @Provides
  fun provideConnectivity(): Connectivity = createConnectivity()
}

expect fun createConnectivity(): Connectivity
