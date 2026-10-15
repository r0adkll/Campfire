// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.navigator

import app.campfire.common.compose.window.WindowScreenLauncher
import app.campfire.common.screens.WindowScreen
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.InterceptedResult
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor

/** Opens [WindowScreen]s in their own window instead of pushing them onto a back stack. */
class WindowScreenInterceptor(
  private val launcher: WindowScreenLauncher,
) : NavigationInterceptor {

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    if (screen !is WindowScreen) return NavigationInterceptor.Skipped
    launcher.launch(screen)
    return NavigationInterceptor.SuccessConsumed
  }
}
