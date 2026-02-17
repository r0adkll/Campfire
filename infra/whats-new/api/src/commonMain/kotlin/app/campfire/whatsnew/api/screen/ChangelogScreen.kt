package app.campfire.whatsnew.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize

@Parcelize
data object ChangelogScreen : BaseScreen(name = "Changelog") {
  override val presentation: Presentation
    get() = Presentation.Fullscreen
}
