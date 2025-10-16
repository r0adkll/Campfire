package app.campfire.analytics.mixpanel

expect class MixPanelFacade {

  val isOptOut: Boolean
  fun optIn()
  fun optOut()

  fun identify(distinctId: String, usePeople: Boolean = true)

  fun track(eventName: String, properties: Map<String, Any?>? = null)
}
