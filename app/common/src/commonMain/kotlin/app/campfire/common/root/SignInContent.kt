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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.LocalWindowChromeInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.currentWindowSizeClass
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.di.UserComponent
import app.campfire.common.root.ui.LoggedOutUi
import app.campfire.common.screens.LoginScreen
import app.campfire.core.session.UserSession
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.colorScheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.foundation.rememberDefaultCircuitSaver
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import dev.zacsweers.metro.Inject

/**
 * The login form on its own, for windows that only sign in: Android Automotive OS opens Campfire
 * just to sign in or change settings, and browses and plays through the media session.
 *
 * It starts on the form, skipping the welcome screen, or on signing back in when the account
 * needs it. [onSignedIn] fires once someone is signed in, straight away if they already are, and
 * [onClose] when the user backs out of the form.
 */
@Inject
class SignInContent(
  private val themeSettings: ThemeSettings,
  private val userSessionManager: UserSessionManager,
) {
  @Composable
  operator fun invoke(
    onSignedIn: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
  ) = StandaloneWindow(userSessionManager) { userComponent, windowInsets ->
    when (val session = userComponent.currentUserSession) {
      is UserSession.LoggedIn -> LaunchedEffect(session.key) { onSignedIn() }
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
}

/**
 * A window showing one part of the app on its own, outside the app's root: the window's size
 * class, chrome insets and retained state around the current user's component.
 */
@Composable
internal fun StandaloneWindow(
  userSessionManager: UserSessionManager,
  content: @Composable (UserComponent, WindowInsets) -> Unit,
) {
  val windowInsets = WindowInsets.systemBars
    .exclude(WindowInsets.statusBars)
    .exclude(WindowInsets.navigationBars)

  CompositionLocalProvider(
    LocalWindowSizeClass provides currentWindowSizeClass(),
    LocalWindowChromeInsets provides windowInsets,
    LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry(),
  ) {
    UserComponentContent(userSessionManager) { userComponent ->
      content(userComponent, windowInsets)
    }
  }
}

/** The login form, for a [StandaloneWindow] whose user isn't signed in. */
@Composable
internal fun SignInWindow(
  userComponent: UserComponent,
  session: UserSession,
  windowInsets: WindowInsets,
  themeSettings: ThemeSettings,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Both offer a back button: the form is all this window shows, and some cars have no back key
  val rootScreen = when (session) {
    is UserSession.NeedsAuthentication -> LoginScreen.ReAuthentication(session.server, canClose = true)
    else -> LoginScreen.Additional
  }
  val backStack = key(session) {
    rememberSaveableBackStack(rootScreen, circuitSaver = rememberDefaultCircuitSaver())
  }
  val baseNavigator = key(session) { rememberCircuitNavigator(backStack) { onClose() } }
  val navigator = rememberInterceptingNavigator(
    navigator = baseNavigator,
    eventListeners = userComponent.navigationEventListeners,
  )

  CircuitCompositionLocals(userComponent.circuit) {
    CampfireTheme(
      colorScheme = { colorScheme(AppTheme.Fixed.Tent) },
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
