// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize

@Parcelize
data object DiscoverScreen : BaseScreen(name = "Discover") {
  override val presentation: Presentation
    get() = Presentation(hideBottomNav = true)
}
