// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.Presentation
import app.campfire.core.parcelize.Parcelize

/**
 * Asks the user to confirm a setup link before anything in it is saved. The link itself waits in
 * the [app.campfire.auth.api.ConnectLinkInbox] under [linkId], so its service token never lands
 * in saved navigation state.
 */
@Parcelize
data class ConnectScreen(val linkId: Long) : BaseScreen("Connect") {
  override val presentation: Presentation get() = Presentation.Fullscreen
}
