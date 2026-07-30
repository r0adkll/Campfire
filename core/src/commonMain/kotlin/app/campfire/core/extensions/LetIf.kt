// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> T.fluentIf(condition: Boolean, block: T.() -> T): T {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }
  return if (condition) block() else this
}
