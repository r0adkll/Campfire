// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl

import app.campfire.core.lifecycle.AppLifecycleState
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.jordond.connectivity.Connectivity
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

class ConnectionDemandTest {

  private val lifecycle = MutableStateFlow(AppLifecycleState.Background)
  private val connectivity = FakeConnectivity(Connectivity.Status.Connected(metered = false))

  @Test
  fun `a background process never demands the socket`() = runTest {
    connectionDemand(lifecycle, connectivity).test {
      assertThat(awaitItem()).isFalse()
      expectNoEvents()
    }
  }

  @Test
  fun `foreground with a network demands the socket`() = runTest {
    connectionDemand(lifecycle, connectivity).test {
      assertThat(awaitItem()).isFalse()

      lifecycle.value = AppLifecycleState.Foreground
      assertThat(awaitItem()).isTrue()

      lifecycle.value = AppLifecycleState.Background
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `losing the network withdraws demand and regaining it restores it`() = runTest {
    lifecycle.value = AppLifecycleState.Foreground

    connectionDemand(lifecycle, connectivity).test {
      assertThat(awaitItem()).isTrue()

      connectivity.emit(Connectivity.Status.Disconnected)
      assertThat(awaitItem()).isFalse()

      connectivity.emit(Connectivity.Status.Connected(metered = true))
      assertThat(awaitItem()).isTrue()
    }
  }

  @Test
  fun `capability churn on a live network does not re-trigger demand`() = runTest {
    lifecycle.value = AppLifecycleState.Foreground

    connectionDemand(lifecycle, connectivity).test {
      assertThat(awaitItem()).isTrue()

      connectivity.emit(Connectivity.Status.Connected(metered = true))
      connectivity.emit(Connectivity.Status.Connected(metered = false))
      expectNoEvents()
    }
  }

  private class FakeConnectivity(initial: Connectivity.Status) : Connectivity {
    private var current = initial
    private val updates = MutableSharedFlow<Connectivity.Status>(replay = 1).apply { tryEmit(initial) }

    override val statusUpdates: SharedFlow<Connectivity.Status> = updates
    override val monitoring: StateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun status(): Connectivity.Status = current

    suspend fun emit(status: Connectivity.Status) {
      current = status
      updates.emit(status)
    }

    override fun start() = Unit
    override fun stop() = Unit
    override fun close() = Unit
  }
}
