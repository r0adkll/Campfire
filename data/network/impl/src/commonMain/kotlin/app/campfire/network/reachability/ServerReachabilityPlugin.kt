// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.io.IOException

/** Thrown in place of a request while the server is known to be unreachable. */
class ServerUnreachableException(origin: String) : IOException("Server $origin is unreachable; skipped request")

/**
 * Feeds the [ReachabilityGate] from the user client's traffic and short-circuits requests
 * while the server is known to be unreachable, so screens fall back to cached data immediately
 * instead of after a connect timeout.
 */
internal fun serverReachabilityPlugin(
  reachability: ReachabilityGate,
): ClientPlugin<Unit> = createClientPlugin("ServerReachability") {
  on(Send) { request ->
    val origin = serverOrigin(request.url.build())
    if (reachability.shouldFailFast(origin)) throw ServerUnreachableException(origin)

    try {
      proceed(request).also { reachability.reachable(origin) }
    } catch (e: Throwable) {
      if (e.isConnectFailure()) reachability.unreachable(origin)
      throw e
    }
  }
}

/**
 * Whether [this] means no connection to the server could be established — as opposed to a slow
 * or interrupted response, which says nothing about reachability.
 */
internal fun Throwable.isConnectFailure(): Boolean =
  this is ConnectTimeoutException || isPlatformConnectFailure()

internal expect fun Throwable.isPlatformConnectFailure(): Boolean
