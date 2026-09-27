// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class IosWidgetPinRequestor : WidgetPinRequester {
  override fun requestPinWidget() {
  }
}
