package app.campfire.analytics.mixpanel

actual class MixPanelFacade {

  actual val isOptOut: Boolean
    get() = false

  actual fun optIn() {
  }

  actual fun optOut() {
  }

  actual fun identify(distinctId: String, usePeople: Boolean) {
  }

  actual fun track(eventName: String, properties: Map<String, Any?>?) {
  }
}
