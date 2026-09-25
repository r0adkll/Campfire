// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException

internal actual fun Throwable.isPlatformConnectFailure(): Boolean =
  this is ConnectException || this is NoRouteToHostException || this is UnknownHostException
