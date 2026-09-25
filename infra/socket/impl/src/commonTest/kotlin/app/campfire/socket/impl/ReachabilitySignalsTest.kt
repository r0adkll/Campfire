// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl

import app.campfire.network.reachability.Reachability
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class ReachabilitySignalsTest {

  @Test
  fun `an initially reachable server does not force a reconnect`() = runTest {
    val signals = reachabilitySignals(flowOf(Reachability.Reachable)).toList()

    assertThat(signals).containsExactly(ReachabilitySignal.Fast)
  }

  @Test
  fun `an unreachable server slows the socket down`() = runTest {
    val signals = reachabilitySignals(flowOf(Reachability.Unknown, Reachability.Unreachable)).toList()

    assertThat(signals).containsExactly(ReachabilitySignal.Fast, ReachabilitySignal.Slow)
  }

  @Test
  fun `the server coming back reconnects the socket`() = runTest {
    val signals = reachabilitySignals(
      flowOf(Reachability.Unreachable, Reachability.Reachable),
    ).toList()

    assertThat(signals).containsExactly(ReachabilitySignal.Slow, ReachabilitySignal.Reconnect)
  }

  @Test
  fun `a network change resets the socket to its fast ramp`() = runTest {
    val signals = reachabilitySignals(
      flowOf(Reachability.Unreachable, Reachability.Unknown, Reachability.Reachable),
    ).toList()

    assertThat(signals).containsExactly(
      ReachabilitySignal.Slow,
      ReachabilitySignal.Fast,
      ReachabilitySignal.Reconnect,
    )
  }
}
