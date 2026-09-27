// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.LocalWindowChromeInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.root.automation.AutomationDeepLinks
import app.campfire.common.root.ui.LoggedInWindow
import app.campfire.common.root.ui.LoggedOutWindow
import app.campfire.core.navigation.DeepLink
import app.campfire.core.session.UserSession
import app.campfire.settings.api.CampfireSettings
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.ThemeManager
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import dev.zacsweers.metro.Inject

/** The app's root content, drawn inside the given window insets (desktop windows). */
@Inject
class CampfireContentWithInsets(
  private val settings: CampfireSettings,
  private val userSessionManager: UserSessionManager,
  private val themeManager: ThemeManager,
  private val themeRepository: AppThemeRepository,
  private val automationDeepLinks: AutomationDeepLinks,
) {
  @Composable
  operator fun invoke(
    onRootPop: () -> Unit,
    windowInsets: WindowInsets,
    deepLink: DeepLink,
    modifier: Modifier = Modifier,
  ) = CampfireRoot(
    onRootPop = onRootPop,
    windowInsets = windowInsets,
    deepLink = deepLink,
    settings = settings,
    userSessionManager = userSessionManager,
    themeManager = themeManager,
    themeRepository = themeRepository,
    automationDeepLinks = automationDeepLinks,
    modifier = modifier,
  )
}

/** The app's root content, inset by the system bars (Android and iOS). */
@Inject
class CampfireContent(
  private val settings: CampfireSettings,
  private val userSessionManager: UserSessionManager,
  private val themeManager: ThemeManager,
  private val themeRepository: AppThemeRepository,
  private val automationDeepLinks: AutomationDeepLinks,
) {
  @Composable
  operator fun invoke(
    onRootPop: () -> Unit,
    deepLink: DeepLink,
    modifier: Modifier = Modifier,
  ) = CampfireRoot(
    onRootPop = onRootPop,
    windowInsets = WindowInsets.systemBars
      .exclude(WindowInsets.statusBars)
      .exclude(WindowInsets.navigationBars),
    deepLink = deepLink,
    settings = settings,
    userSessionManager = userSessionManager,
    themeManager = themeManager,
    themeRepository = themeRepository,
    automationDeepLinks = automationDeepLinks,
    modifier = modifier,
  )
}

@Composable
private fun CampfireRoot(
  onRootPop: () -> Unit,
  windowInsets: WindowInsets,
  deepLink: DeepLink,
  settings: CampfireSettings,
  userSessionManager: UserSessionManager,
  themeManager: ThemeManager,
  themeRepository: AppThemeRepository,
  automationDeepLinks: AutomationDeepLinks,
  modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(
    LocalWindowSizeClass provides currentWindowSizeClass(),
    LocalWindowChromeInsets provides windowInsets,
    LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry(),
  ) {
    UserComponentContent(userSessionManager) { userComponent ->
      val session = userComponent.currentUserSession
      if (deepLink is DeepLink.Setup && session != UserSession.Loading) {
        LaunchedEffect(deepLink) {
          automationDeepLinks.applySetup(
            setup = deepLink,
            isLoggedIn = session is UserSession.LoggedIn,
          )
        }
      }

      when (session) {
        is UserSession.NeedsAuthentication,
        UserSession.LoggedOut,
        -> LoggedOutWindow(
          userComponent = userComponent,
          onRootPop = onRootPop,
          windowInsets = windowInsets,
          settings = settings,
        )

        is UserSession.LoggedIn -> LoggedInWindow(
          userComponent = userComponent,
          onRootPop = onRootPop,
          deepLink = deepLink,
          settings = settings,
          themeManager = themeManager,
          themeRepository = themeRepository,
        )

        UserSession.Loading -> Unit
      }
    }
  }
}
