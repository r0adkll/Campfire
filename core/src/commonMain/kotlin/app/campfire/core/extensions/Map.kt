// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

inline fun <KeyT, ValueT> Map<KeyT, ValueT>.ifNotEmpty(block: Map<KeyT, ValueT>.() -> Unit) {
  if (isNotEmpty()) block() else Unit
}
