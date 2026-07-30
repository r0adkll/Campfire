// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

// TODO: Find iOS equivalent to UnknownHostException
actual val Throwable.isUnknownHostException: Boolean
  get() = message?.contains(NETWORK_ADDRESS_REGEX) == true ||
    this.cause?.isUnknownHostException ?: false
