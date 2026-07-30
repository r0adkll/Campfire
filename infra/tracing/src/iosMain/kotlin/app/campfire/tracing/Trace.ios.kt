// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.tracing

actual object Trace {
  actual val isEnabled: Boolean
    get() = false

  actual fun beginSection(label: String) {
  }

  actual fun endSection() {
  }

  actual fun beginAsyncSection(methodName: String, cookie: Int) {
  }

  actual fun endAsyncSection(methodName: String, cookie: Int) {
  }

  actual fun beginAsyncSectionWithTrackName(trackName: String, methodName: String, cookie: Int) {
  }

  actual fun endAsyncSectionWithTrackName(trackName: String, methodName: String, cookie: Int) {
  }
}
