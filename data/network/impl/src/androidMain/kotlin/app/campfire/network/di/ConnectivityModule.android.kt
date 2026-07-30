// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.di

import dev.jordond.connectivity.Connectivity

actual fun createConnectivity(): Connectivity {
  return Connectivity {
    autoStart = true
  }
}
