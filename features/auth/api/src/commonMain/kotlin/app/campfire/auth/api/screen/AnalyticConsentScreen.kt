// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize

@Parcelize
data object AnalyticConsentScreen : BaseScreen("AnalyticConsent") {
  override val presentation: Presentation get() = Presentation.Fullscreen
}
