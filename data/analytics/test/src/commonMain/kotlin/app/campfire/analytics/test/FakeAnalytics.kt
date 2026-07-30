// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.analytics.test

import app.campfire.analytics.Analytics
import app.campfire.analytics.events.AnalyticEvent

/**
 * Test fake for [Analytics] interface to be used in tests
 */
class FakeAnalytics : Analytics {

  val events = mutableListOf<AnalyticEvent>()

  override var debugState: String = ""

  override fun send(event: AnalyticEvent) {
    events.add(event)
  }
}
