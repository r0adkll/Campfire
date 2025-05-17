package app.campfire.common.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.campfire.account.api.UserSessionManager
import app.campfire.account.ui.rememberCurrentTent
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.PlatformBackHandler
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.extensions.shouldUseDynamicColors
import app.campfire.common.compose.session.LocalPlaybackSession
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.navigator.OpenUrlNavigator
import app.campfire.core.logging.bark
import app.campfire.settings.api.CampfireSettings
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.continuityRetainedStateRegistry
import com.slack.circuit.runtime.Navigator
import dev.zacsweers.metro.Inject

@Inject
class CampfireContentProvider(
  private val settings: CampfireSettings,
  private val userSessionManager: UserSessionManager,
) {

  @Composable
  fun Content(
    onRootPop: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier,
  ) {
    CampfireContent(
      onRootPop = onRootPop,
      onOpenUrl = onOpenUrl,
      settings = settings,
      userSessionManager = userSessionManager,
      modifier = modifier,
    )
  }

  @Composable
  fun ContentWithInsets(
    onRootPop: () -> Unit,
    onOpenUrl: (String) -> Unit,
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
  ) {
    CampfireContentWithInsets(
      onRootPop = onRootPop,
      onOpenUrl = onOpenUrl,
      windowInsets = windowInsets,
      settings = settings,
      userSessionManager = userSessionManager,
      modifier = modifier,
    )
  }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Inject
@Composable
internal fun CampfireContentWithInsets(
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  windowInsets: WindowInsets,
  settings: CampfireSettings,
  userSessionManager: UserSessionManager,
  modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(
    LocalWindowSizeClass provides calculateWindowSizeClass(),
    LocalRetainedStateRegistry provides continuityRetainedStateRegistry(),
  ) {
    // TODO: We are re-shifting scopes, so this will need to be reworked
    UserComponentContent(
      userSessionManager = userSessionManager,
    ) { userComponent ->

      val backStack = key(userComponent.currentUserSession) { rememberSaveableBackStack(userComponent.rootScreen) }
      val navigator = key(userComponent.currentUserSession) { rememberCircuitNavigator(backStack) { onRootPop() } }

      LaunchedEffect(backStack, navigator) {
        bark {
          """
            UserComponentContent(session=${userComponent.currentUserSession})
              backStack = $backStack,
              navigator = $navigator,
            )
          """.trimIndent()
        }
      }

      // Observe Current Session
      val currentSession by remember(userComponent) {
        userComponent.sessionsRepository.observeCurrentSession()
      }.collectAsState(null)

      PlatformBackHandler(
        enabled = backStack.size > 1,
        onBack = {
          // Check the backStack on each call as the `BackHandler` enabled state only updates on composition
          if (backStack.size > 1) {
            navigator.pop()
          }
        },
      )

      val urlNavigator: Navigator = remember(navigator) {
        OpenUrlNavigator(navigator, onOpenUrl)
      }

      CircuitCompositionLocals(userComponent.circuit) {
        CampfireTheme(
          tent = rememberCurrentTent(userComponent.currentUserSession),
          useDarkColors = settings.shouldUseDarkColors(),
          useDynamicColors = settings.shouldUseDynamicColors(),
        ) {
          CompositionLocalProvider(
            LocalPlaybackSession provides currentSession,
          ) {
            HomeUi(
              backstack = backStack,
              navigator = urlNavigator,
              windowInsets = windowInsets,
              modifier = modifier,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun CampfireContent(
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  settings: CampfireSettings,
  userSessionManager: UserSessionManager,
  modifier: Modifier = Modifier,
) {
  CampfireContentWithInsets(
    onRootPop = onRootPop,
    settings = settings,
    userSessionManager = userSessionManager,
    onOpenUrl = onOpenUrl,
    windowInsets = WindowInsets.systemBars.exclude(WindowInsets.statusBars),
    modifier = modifier,
  )
}
