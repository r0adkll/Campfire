// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.navigator

import app.campfire.common.screens.WorkspaceScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.navigation.intercepting.InterceptedResult
import com.slack.circuitx.navigation.intercepting.NavigationContext
import com.slack.circuitx.navigation.intercepting.NavigationInterceptor

/**
 * Sends [WorkspaceScreen]s opened from the supporting pane to the app's main area, [rootNavigator],
 * leaving the pane to show what they're about.
 */
class WorkspaceScreenInterceptor(
  private val rootNavigator: Navigator,
) : NavigationInterceptor {

  override fun goTo(screen: Screen, navigationContext: NavigationContext): InterceptedResult {
    if (screen !is WorkspaceScreen) return NavigationInterceptor.Skipped
    rootNavigator.goTo(screen)
    return NavigationInterceptor.SuccessConsumed
  }
}
