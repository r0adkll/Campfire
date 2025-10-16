package app.campfire.analytics

import app.campfire.analytics.events.AnalyticEvent

class FakeAnalytics : Analytics {

  val events = mutableListOf<AnalyticEvent>()

  override fun send(event: AnalyticEvent) {
    events += event
  }
}
