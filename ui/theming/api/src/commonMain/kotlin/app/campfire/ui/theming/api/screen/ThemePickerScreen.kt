// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize

@Parcelize
data object ThemePickerScreen : BaseScreen(name = "ThemePicker") {
  override val presentation: Presentation
    get() = Presentation(
      hideBottomNav = true,
      hidePlaybackBar = true,
    )
}
