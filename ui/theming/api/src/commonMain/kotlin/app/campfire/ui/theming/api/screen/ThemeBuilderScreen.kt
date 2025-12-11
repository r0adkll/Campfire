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
