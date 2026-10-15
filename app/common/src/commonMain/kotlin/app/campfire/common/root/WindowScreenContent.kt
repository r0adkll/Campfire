// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.screens.UrlScreen
import app.campfire.common.screens.WindowScreen
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.colorScheme
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavEvent
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import dev.zacsweers.metro.Inject

/**
 * A [WindowScreen] in a window of its own: the window's size class and retained-state registry,
 * the signed-in user's component and the app theme around the screen's Circuit UI.
 *
 * [onClose] fires when the screen pops itself, and when the window has nothing to show: the
 * user signed out or switched to another account since it opened.
 */
@Inject
class WindowScreenContent(
  private val themeSettings: ThemeSettings,
  private val userSessionManager: UserSessionManager,
  private val themeRepository: AppThemeRepository,
) {
  @Composable
  operator fun invoke(
    screen: WindowScreen,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
  ) = WindowScreenRoot(
    screen = screen,
    onClose = onClose,
    themeSettings = themeSettings,
    userSessionManager = userSessionManager,
    themeRepository = themeRepository,
    modifier = modifier,
  )
}

@Composable
private fun WindowScreenRoot(
  screen: WindowScreen,
  onClose: () -> Unit,
  themeSettings: ThemeSettings,
  userSessionManager: UserSessionManager,
  themeRepository: AppThemeRepository,
  modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(
    LocalWindowSizeClass provides currentWindowSizeClass(),
    LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry(),
  ) {
    UserComponentContent(userSessionManager) { userComponent ->
      val session = userComponent.currentUserSession
      // The account the window opened for, kept across process death
      val userId = rememberSaveable { (session as? UserSession.LoggedIn)?.requiredUserId.orEmpty() }
      if (session !is UserSession.LoggedIn || session.requiredUserId != userId) {
        LaunchedEffect(Unit) { onClose() }
        return@UserComponentContent
      }

      val appTheme by remember {
        themeRepository.observeCurrentAppTheme()
      }.collectAsState()

      val uriHandler = LocalUriHandler.current
      CircuitCompositionLocals(userComponent.circuit) {
        CampfireTheme(
          colorScheme = { colorScheme(appTheme) },
          useDarkColors = themeSettings.shouldUseDarkColors(),
        ) {
          // Sheets the screen opens draw over it, in this window
          ContentWithOverlays(modifier = modifier) {
            CircuitContent(
              screen = screen,
              onNavEvent = { event ->
                when (event) {
                  is NavEvent.Pop -> onClose()
                  is NavEvent.GoTo -> (event.screen as? UrlScreen)?.let { uriHandler.openUri(it.url) }
                  else -> Unit
                }
              },
            )
          }
        }
      }
    }
  }
}
