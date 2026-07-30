// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.toast

import android.os.Handler
import android.os.Looper

actual inline fun runInMainThread(crossinline block: () -> Unit) {
  Handler(Looper.getMainLooper()).post { block() }
}
