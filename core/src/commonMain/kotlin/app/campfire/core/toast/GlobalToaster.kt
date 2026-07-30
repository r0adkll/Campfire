// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.toast

/**
 * An easy to use global singleton
 */
object GlobalToaster {

  /*
   * 🐲 WARNING!
   *
   * Using a singleton like this can be prone to memory leaks if you are not careful to unregister this delegate
   * when its underlying reference (i.e. Activities, etc) are destroyed.
   */
  private var delegate: Toast? = null

  fun show(
    message: String,
    duration: Toast.Duration,
  ) {
    runInMainThread { delegate?.show(message, duration) }
  }

  fun register(delegate: Toast) {
    this.delegate = delegate
  }

  fun unregister() {
    this.delegate = null
  }
}

expect inline fun runInMainThread(crossinline block: () -> Unit)
