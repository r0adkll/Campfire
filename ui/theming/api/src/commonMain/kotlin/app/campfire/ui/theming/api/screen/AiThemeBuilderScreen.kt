// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api.screen

import app.campfire.common.screens.DetailScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.HalogenStyle

@Parcelize
data class AiThemeBuilderScreen(
  val id: String? = null,
  val prompt: String? = null,
  val themeName: String? = null,
  val style: HalogenStyle = HalogenStyle.Expressive,
) : DetailScreen(name = "AiThemeBuilder") {
  constructor(theme: AppTheme.Fixed.Ai) : this(
    id = theme.id,
    prompt = theme.prompt,
    themeName = theme.name,
    style = theme.style,
  )

  override val presentation: Presentation
    get() = Presentation(
      hideBottomNav = true,
      hidePlaybackBar = true,
      isDetailScreen = true,
    )
}
