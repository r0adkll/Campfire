// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.di.UserComponent
import app.campfire.common.navigator.OpenUrlNavigator
import app.campfire.common.root.ui.LoggedOutUi
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.session.UserSession
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.colorScheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.foundation.rememberDefaultCircuitSaver
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import dev.zacsweers.metro.Inject

/**
 * Settings on their own, for the window Android Automotive OS opens from Campfire's settings
 * button. Signed out, it shows the login form instead, and Settings once someone signs in.
 * [onClose] fires when the user backs out of either.
 */
@Inject
class SettingsWindowContent(
  private val themeSettings: ThemeSettings,
  private val themeRepository: AppThemeRepository,
  private val userSessionManager: UserSessionManager,
) {
  @Composable
  operator fun invoke(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
  ) = StandaloneWindow(userSessionManager) { userComponent, windowInsets ->
    when (val session = userComponent.currentUserSession) {
      is UserSession.LoggedIn -> SettingsWindow(
        userComponent = userComponent,
        session = session,
        windowInsets = windowInsets,
        onClose = onClose,
        modifier = modifier,
      )
      else -> SignInWindow(
        userComponent = userComponent,
        session = session,
        windowInsets = windowInsets,
        themeSettings = themeSettings,
        onClose = onClose,
        modifier = modifier,
      )
    }
  }

  @Composable
  private fun SettingsWindow(
    userComponent: UserComponent,
    session: UserSession.LoggedIn,
    windowInsets: WindowInsets,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
  ) {
    val backStack = key(session.key) {
      rememberSaveableBackStack(SettingsScreen(), circuitSaver = rememberDefaultCircuitSaver())
    }
    val baseNavigator = key(session.key) { rememberCircuitNavigator(backStack) { onClose() } }
    val interceptingNavigator = rememberInterceptingNavigator(
      navigator = baseNavigator,
      eventListeners = userComponent.navigationEventListeners,
    )
    // Links (privacy policy, terms) open in whatever can show them, rather than as a screen
    val uriHandler = LocalUriHandler.current
    val navigator = remember(interceptingNavigator, uriHandler) { OpenUrlNavigator(interceptingNavigator, uriHandler) }

    val appTheme by remember { themeRepository.observeCurrentAppTheme() }.collectAsState()

    CircuitCompositionLocals(userComponent.circuit) {
      CampfireTheme(
        colorScheme = { colorScheme(appTheme) },
        useDarkColors = themeSettings.shouldUseDarkColors(),
      ) {
        CompositionLocalProvider(LocalContentLayout provides ContentLayout.Root) {
          LoggedOutUi(
            backstack = backStack,
            navigator = navigator,
            windowInsets = windowInsets,
            modifier = modifier,
          )
        }
      }
    }
  }
}
