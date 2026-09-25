// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl

import app.campfire.network.reachability.Reachability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** How the socket should adjust its reconnect loop to what the app knows about the server. */
internal enum class ReachabilitySignal {
  /** Nothing known against the server: ramp reconnects up from the short initial delay. */
  Fast,

  /**
   * The server is known to be unreachable: wait the maximum delay between attempts. The socket
   * keeps retrying rather than stopping — HTTP only probes when something makes a request, so
   * the socket's attempts are what notice the server's return while the app sits idle.
   */
  Slow,

  /** The server just became reachable: reconnect now with a fresh backoff. */
  Reconnect,
}

/**
 * Maps [ServerReachability][app.campfire.network.reachability.ServerReachability] updates onto
 * [ReachabilitySignal]s. [ReachabilitySignal.Reconnect] fires only on a transition *into*
 * [Reachability.Reachable] — the initial state is never a transition, so it can't race the
 * socket's own first open.
 */
internal fun reachabilitySignals(status: Flow<Reachability>): Flow<ReachabilitySignal> = flow {
  var previous: Reachability? = null
  status.collect { current ->
    val signal = when (current) {
      Reachability.Unreachable -> ReachabilitySignal.Slow
      Reachability.Unknown -> ReachabilitySignal.Fast
      Reachability.Reachable -> if (previous != null && previous != Reachability.Reachable) {
        ReachabilitySignal.Reconnect
      } else {
        ReachabilitySignal.Fast
      }
    }
    previous = current
    emit(signal)
  }
}
