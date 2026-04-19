package app.campfire.common.root.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.LocalItemCardMarquee
import app.campfire.common.di.UserComponent
import app.campfire.settings.api.CampfireSettings
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.colorScheme
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator

@Composable
internal fun LoggedOutWindow(
  userComponent: UserComponent,
  onRootPop: () -> Unit,
  windowInsets: WindowInsets,
  settings: CampfireSettings,
  modifier: Modifier = Modifier,
) {
  val backStack = key(userComponent.currentUserSession) {
    rememberSaveableBackStack(userComponent.rootScreen())
  }

  val baseNavigator = key(userComponent.currentUserSession) { rememberCircuitNavigator(backStack) { onRootPop() } }
  val navigator = rememberInterceptingNavigator(
    navigator = baseNavigator,
    eventListeners = userComponent.navigationEventListeners,
  )

  CircuitCompositionLocals(userComponent.circuit) {
    CampfireTheme(
      colorScheme = { colorScheme(AppTheme.Fixed.Tent) },
      useDarkColors = settings.shouldUseDarkColors(),
    ) {
      // Observe here and wire as composition local to avoid N-number of parameter
      // burials to wire all usages of this component
      val itemCardMarqueeEnabled by remember {
        settings.observeLibraryItemMarqueeEnabled()
      }.collectAsState()

      CompositionLocalProvider(
        LocalItemCardMarquee provides itemCardMarqueeEnabled,
        LocalContentLayout provides ContentLayout.Root,
      ) {
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LoggedOutUi(
  backstack: SaveableBackStack,
  navigator: Navigator,
  windowInsets: WindowInsets,
  modifier: Modifier = Modifier,
) {
  val overlayHost = rememberOverlayHost()
  NavigationBackHandler(
    state = rememberNavigationEventState(NavigationEventInfo.None),
    isBackEnabled = overlayHost.currentOverlayData != null,
    onBackCompleted = {
      overlayHost.currentOverlayData?.finish(Unit)
    },
  )

  ContentWithOverlays(
    overlayHost = overlayHost,
    modifier = modifier,
  ) {
    Scaffold(
      contentWindowInsets = windowInsets,
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      ) {
        SharedElementTransitionLayout {
          NavigableCircuitContent(
            navigator = navigator,
            backStack = backstack,
            decoratorFactory = remember(navigator) {
              GestureNavigationDecorationFactory(
                onBackInvoked = navigator::pop,
              )
            },
          )
        }
      }
    }
  }
}
