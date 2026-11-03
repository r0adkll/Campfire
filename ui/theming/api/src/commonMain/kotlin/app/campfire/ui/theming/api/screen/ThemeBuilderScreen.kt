// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize
import app.campfire.ui.theming.api.AppTheme

/**
 * The custom theme builder, editing the theme saved as [customThemeId], or making a new one.
 *
 * @param seedColor for a new theme, the ARGB color it starts from, such as a built-in theme's
 *   main color when the user customizes it.
 * @param icon for a new theme, the icon it starts with.
 */
@Parcelize
data class ThemeBuilderScreen(
  val customThemeId: String? = null,
  val seedColor: Int? = null,
  val icon: AppTheme.Icon? = null,
) : BaseScreen(name = "ThemeBuilder") {
  override val presentation: Presentation
    get() = Presentation(
      hideBottomNav = true,
      hidePlaybackBar = true,
    )
}
