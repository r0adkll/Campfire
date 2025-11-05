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
