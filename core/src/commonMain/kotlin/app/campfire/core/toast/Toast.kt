// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.toast

fun interface Toast {
  enum class Duration {
    SHORT, LONG
  }

  fun show(message: String, duration: Duration): ToastHandle
}

fun interface ToastHandle {
  fun cancel()
}
