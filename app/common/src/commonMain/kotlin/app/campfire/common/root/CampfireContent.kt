package app.campfire.common.root

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.session.LocalPlaybackSession
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.util.LocalThemeDispatcher
import app.campfire.common.compose.util.ThemeDispatcher
import app.campfire.common.navigator.OpenUrlNavigator
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.ui.theming.api.colorScheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.lifecycleRetainedStateRegistry
import com.slack.circuit.runtime.Navigator
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias CampfireContentWithInsets = @Composable (
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  windowInsets: WindowInsets,
  modifier: Modifier,
) -> Unit

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Inject
@Composable
fun CampfireContentWithInsets(
  @Assisted onRootPop: () -> Unit,
  @Assisted onOpenUrl: (String) -> Unit,
  @Assisted windowInsets: WindowInsets,
  settings: CampfireSettings,
  userSessionManager: UserSessionManager,
  themeManager: ThemeManager,
  themeSettings: ThemeSettings,
  themeRepository: AppThemeRepository,
  @Assisted modifier: Modifier = Modifier,
) {
  val appUriHandler = remember(onOpenUrl) {
    object : UriHandler {
      override fun openUri(uri: String) {
        onOpenUrl(uri)
      }
    }
  }

  CompositionLocalProvider(
    LocalWindowSizeClass provides calculateWindowSizeClass(),
    LocalRetainedStateRegistry provides lifecycleRetainedStateRegistry(),
    LocalUriHandler provides appUriHandler,
  ) {
    UserComponentContent(userSessionManager) { userComponent ->
      val backStack = key(userComponent.currentUserSession) { rememberSaveableBackStack(userComponent.rootScreen()) }
      val baseNavigator = key(userComponent.currentUserSession) { rememberCircuitNavigator(backStack) { onRootPop() } }
      val navigator = rememberInterceptingNavigator(
        navigator = baseNavigator,
        eventListeners = userComponent.navigationEventListeners,
      )

      // Observe Current Session
      val currentSession by remember(userComponent) {
        userComponent.sessionsRepository.observeCurrentSession()
      }.collectAsState(null)

      val urlNavigator: Navigator = remember(navigator) {
        OpenUrlNavigator(navigator, onOpenUrl)
      }

      // Remember an instance of the theme dispatcher
      val themeManagerDispatcher = remember {
        ThemeDispatcher { key, imageBitmap ->
          themeManager.enqueue(
            key = key,
            image = imageBitmap,
          )
        }
      }

      val appTheme by remember {
        themeRepository.observeCurrentAppTheme()
      }.collectAsState()

      CircuitCompositionLocals(userComponent.circuit) {
        CampfireTheme(
          colorScheme = { colorScheme(appTheme) },
          useDarkColors = settings.shouldUseDarkColors(),
        ) {
          CompositionLocalProvider(
            LocalPlaybackSession provides currentSession,
            LocalThemeDispatcher provides themeManagerDispatcher,
          ) {
            RootUi(
              backstack = backStack,
              navigator = urlNavigator,
              themeManager = themeManager,
              themeSettings = themeSettings,
              windowInsets = windowInsets,
              navigationEventListeners = userComponent.navigationEventListeners,
              modifier = modifier,
            )
          }
        }
      }
    }
  }
}

typealias CampfireContent = @Composable (
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  modifier: Modifier,
) -> Unit

@Inject
@Composable
fun CampfireContent(
  @Assisted onRootPop: () -> Unit,
  @Assisted onOpenUrl: (String) -> Unit,
  settings: CampfireSettings,
  userSessionManager: UserSessionManager,
  themeManager: ThemeManager,
  themeSettings: ThemeSettings,
  themeRepository: AppThemeRepository,
  @Assisted modifier: Modifier = Modifier,
) {
  CampfireContentWithInsets(
    onRootPop = onRootPop,
    settings = settings,
    userSessionManager = userSessionManager,
    themeManager = themeManager,
    themeSettings = themeSettings,
    themeRepository = themeRepository,
    onOpenUrl = onOpenUrl,
    windowInsets = WindowInsets.systemBars
      .exclude(WindowInsets.statusBars)
      .exclude(WindowInsets.navigationBars),
    modifier = modifier,
  )
}
