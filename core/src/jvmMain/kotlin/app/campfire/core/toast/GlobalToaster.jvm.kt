// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.toast

actual inline fun runInMainThread(crossinline block: () -> Unit) {
  block()
}
