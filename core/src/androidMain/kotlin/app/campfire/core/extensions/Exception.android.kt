// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import java.net.UnknownHostException

actual val Throwable.isUnknownHostException: Boolean
  get() = this is UnknownHostException ||
    message?.contains(NETWORK_ADDRESS_REGEX) == true ||
    this.cause?.isUnknownHostException ?: false
