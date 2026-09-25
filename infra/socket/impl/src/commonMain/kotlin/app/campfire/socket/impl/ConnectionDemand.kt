// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl

import app.campfire.core.lifecycle.AppLifecycleState
import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Whether the realtime socket should be connected: only while the app is visible AND the
 * device has a network. Socket events only refresh on-screen data, so there is nothing to gain
 * from dialing the server in the background or with no route to it — and socket.io's reconnect
 * loop would otherwise keep the radio awake the whole time.
 *
 * Connectivity is reduced to connected/disconnected before de-duplicating: the platform monitor
 * re-emits on every capability change (signal strength, metered flips), and each of those must
 * not reset the socket's backoff.
 */
internal fun connectionDemand(
  lifecycle: Flow<AppLifecycleState>,
  connectivity: Connectivity,
): Flow<Boolean> {
  val online = connectivity.statusUpdates
    .map { it.isConnected }
    .onStart { emit(connectivity.status().isConnected) }
    .distinctUntilChanged()
  return combine(lifecycle, online) { state, isOnline ->
    state == AppLifecycleState.Foreground && isOnline
  }.distinctUntilChanged()
}
