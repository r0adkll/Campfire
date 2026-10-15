// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningFold

/**
 * Emits whenever the server may have just become reachable again: [status] turning
 * [Reachability.Reachable], [inRange] turning true, or [online] turning true. The initial values
 * are not transitions and never emit. Bursts (a network change typically flips several at once)
 * are collapsed by [settle].
 *
 * Reachability alone isn't enough to trigger on: it only changes when something makes a request,
 * and with the app in the background nothing does. So regaining the network, or moving back onto
 * a network that can reach a local server, triggers too.
 */
@OptIn(FlowPreview::class)
fun reconnectTriggers(
  status: Flow<Reachability>,
  inRange: Flow<Boolean>,
  online: Flow<Boolean>,
  settle: Duration = SETTLE,
): Flow<Unit> = merge(
  status.map { it == Reachability.Reachable }.risingEdges(),
  inRange.risingEdges(),
  online.risingEdges(),
).debounce(settle)

/** Emits each false → true change of [this], ignoring its initial value. */
private fun Flow<Boolean>.risingEdges(): Flow<Unit> = distinctUntilChanged()
  .runningFold(null as Pair<Boolean?, Boolean>?) { previous, current -> previous?.second to current }
  .filter { it != null && it.first == false && it.second }
  .map { }

private val SETTLE = 2.seconds
