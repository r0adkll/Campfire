// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.analytics.mixpanel

expect class MixPanelFacade {

  val debugState: String

  val isOptOut: Boolean
  fun optIn()
  fun optOut()

  fun identify(distinctId: String, usePeople: Boolean = true)

  fun track(eventName: String, properties: Map<String, Any?>? = null)
}
