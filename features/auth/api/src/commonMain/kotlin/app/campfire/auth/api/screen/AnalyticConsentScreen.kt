package app.campfire.auth.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize

@Parcelize
data object AnalyticConsentScreen : BaseScreen("AnalyticConsent") {
  override val presentation: Presentation = Presentation.Fullscreen
}
