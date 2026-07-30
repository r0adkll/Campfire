// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

expect val Throwable.isUnknownHostException: Boolean

internal val NETWORK_ADDRESS_REGEX =
  "(http|ftp|https)://([\\w_-]+(?:\\.[\\w_-]+)+)([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])".toRegex()
