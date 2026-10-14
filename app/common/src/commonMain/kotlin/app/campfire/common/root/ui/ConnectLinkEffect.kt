// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.campfire.auth.api.ConnectLinkInbox
import app.campfire.auth.api.screen.ConnectScreen
import app.campfire.core.navigation.DeepLink
import com.slack.circuit.runtime.Navigator

/**
 * Shows a setup link's confirmation over whatever the window shows. Both the signed-out and
 * signed-in windows host this, and the root keeps handing them the same deep link (after signing
 * in, say), so the [inbox] lets each link through once.
 */
@Composable
internal fun ConnectLinkEffect(
  deepLink: DeepLink,
  inbox: ConnectLinkInbox,
  navigator: Navigator,
) {
  LaunchedEffect(deepLink) {
    if (deepLink is DeepLink.Connect && inbox.deliver(deepLink.nonce, deepLink.result)) {
      navigator.goTo(ConnectScreen(deepLink.nonce))
    }
  }
}
