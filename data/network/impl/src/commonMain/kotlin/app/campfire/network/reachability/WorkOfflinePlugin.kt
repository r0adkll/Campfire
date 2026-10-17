// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.io.IOException

/** Thrown in place of a request while the user is working offline. */
class WorkingOfflineException : IOException("Working offline; skipped request")

/**
 * Fails every request while the user is working offline (see [WorkOffline]). For the clients that
 * stream and download from the server; the user client gets the same from its [ReachabilityGate],
 * which also reports the state as [Reachability.Offline].
 */
internal fun workOfflinePlugin(
  workOffline: WorkOffline,
): ClientPlugin<Unit> = createClientPlugin("WorkOffline") {
  on(Send) { request ->
    if (workOffline.isOffline()) throw WorkingOfflineException()
    proceed(request)
  }
}
