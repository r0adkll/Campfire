package app.campfire.analytics.events

/**
 * Viewing a screen in the app
 * @param screenType the type of screen that is viewed
 */
class ScreenViewEvent(
  screenName: String,
  screenType: ScreenType = ScreenType.Full,
) : AnalyticEvent(
  eventName = "screen_view",
  params = mapOf(
    "screen_name" to screenName,
    "screen_type" to screenType,
  ),
)

enum class ScreenType(val value: String) {
  Full("full"),
  Overlay("overlay"),
  Dialog("dialog"),
}
