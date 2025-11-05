package app.campfire.analytics

import app.campfire.analytics.events.AnalyticEvent
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark

object LoggingAnalytics : Analytics {
  override val debugState: String
    get() = "LoggingAnalytics(enabled=true)"

  override fun send(event: AnalyticEvent) {
    bark(
      priority = LogPriority.INFO,
      tag = "Analytics",
    ) {
      "Event[${event.eventName}]: ${event.params}"
    }
  }
}
