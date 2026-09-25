// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import app.campfire.network.reachability.Reachability
import app.cash.turbine.test
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

class ReconnectTriggersTest {

  private val status = MutableStateFlow(Reachability.Reachable)
  private val inRange = MutableStateFlow(true)
  private val online = MutableStateFlow(true)

  private fun triggers() = reconnectTriggers(status, inRange, online, settle = 2.seconds)

  @Test
  fun `initial values never trigger`() = runTest {
    triggers().test {
      advanceTimeBy(10.seconds)
      expectNoEvents()
    }
  }

  @Test
  fun `the server becoming reachable again triggers`() = runTest {
    triggers().test {
      status.value = Reachability.Unreachable
      status.value = Reachability.Reachable
      awaitItem()
    }
  }

  @Test
  fun `coming back in range triggers`() = runTest {
    triggers().test {
      inRange.value = false
      advanceTimeBy(5.seconds)
      inRange.value = true
      awaitItem()
    }
  }

  @Test
  fun `regaining the network triggers`() = runTest {
    triggers().test {
      online.value = false
      advanceTimeBy(5.seconds)
      online.value = true
      awaitItem()
    }
  }

  @Test
  fun `losing reach never triggers`() = runTest {
    triggers().test {
      status.value = Reachability.OutOfRange
      inRange.value = false
      online.value = false
      advanceTimeBy(10.seconds)
      expectNoEvents()
    }
  }

  @Test
  fun `a network change flipping everything at once triggers one sync`() = runTest {
    status.value = Reachability.OutOfRange
    inRange.value = false
    online.value = false

    triggers().test {
      online.value = true
      inRange.value = true
      status.value = Reachability.Reachable
      awaitItem()
      advanceTimeBy(10.seconds)
      expectNoEvents()
    }
  }
}
