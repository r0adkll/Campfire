package app.campfire.analytics.events

/**
 * This event signifies that the user has viewed a specific screen.
 */
class ScreenView(
  screenName: String,
) : AnalyticEvent(
  eventName = "screen_view",
  params = mapOf(
    "screen_name" to screenName,
  ),
)
