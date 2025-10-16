package app.campfire.analytics.events

sealed class AnalyticEvent(
  val eventName: String,
  val params: Map<String, Any>? = null,
)
