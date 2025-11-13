package app.campfire.network.di

import dev.jordond.connectivity.Connectivity

actual fun createConnectivity(): Connectivity {
  return Connectivity {
    autoStart = true
  }
}
