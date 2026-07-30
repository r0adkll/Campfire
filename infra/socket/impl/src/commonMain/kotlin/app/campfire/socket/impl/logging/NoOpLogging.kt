// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.impl.logging

import com.piasy.kmp.xlog.LoggingImpl

class NoOpLogging : LoggingImpl {
  override fun debug(): Boolean = false
  override fun debug(tag: String, content: String) = Unit
  override fun info(tag: String, content: String) = Unit
  override fun error(tag: String, content: String) = Unit
}
