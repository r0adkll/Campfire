// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.analytics.mixpanel

import app.campfire.analytics.Analytics
import app.campfire.analytics.events.AnalyticEvent

class MixPanelAnalytics(
  private val mixPanelFacade: MixPanelFacade,
) : Analytics {

  override val debugState: String
    get() = mixPanelFacade.debugState

  override fun send(event: AnalyticEvent) {
    mixPanelFacade.track(event.eventName, event.params)
  }
}
