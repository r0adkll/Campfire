// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.analytics.mixpanel

actual class MixPanelFacade {

  actual val debugState: String
    get() = "MixPanel[NO-OP]"

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
