// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize

@Parcelize
data class ThemeBuilderScreen(
  val customThemeId: String? = null,
) : BaseScreen(name = "ThemeBuilder") {
  override val presentation: Presentation
    get() = Presentation(hideBottomNav = true)
}
