// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize

/**
 * Scans a setup link's QR code with the camera, then hands the link to [ConnectScreen] to
 * confirm, the same way an opened link is.
 */
@Parcelize
data object ScanSetupCodeScreen : BaseScreen("ScanSetupCode") {
  override val presentation: Presentation get() = Presentation.Fullscreen
}
