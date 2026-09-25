// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorNotConnectedToInternet

// NSURLSession reports connect and response timeouts alike as NSURLErrorTimedOut, so a timeout
// can't be told apart from a slow server here and is deliberately not treated as unreachable.
internal actual fun Throwable.isPlatformConnectFailure(): Boolean {
  val code = (this as? DarwinHttpRequestException)?.origin?.code ?: return false
  return code == NSURLErrorCannotConnectToHost ||
    code == NSURLErrorCannotFindHost ||
    code == NSURLErrorDNSLookupFailed ||
    code == NSURLErrorNotConnectedToInternet
}
