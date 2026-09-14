// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.navigator

import androidx.compose.ui.platform.UriHandler
import app.campfire.common.screens.UrlScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen

class OpenUrlNavigator(
  private val navigator: Navigator,
  private val uriHandler: UriHandler,
) : Navigator by navigator {

  override fun goTo(screen: Screen): Boolean {
    return when (screen) {
      is UrlScreen -> {
        uriHandler.openUri(screen.url)
        true
      }
      else -> navigator.goTo(screen)
    }
  }
}
