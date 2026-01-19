@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.campfire.common.root

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.campfire.account.ui.picker.AccountPickerResult
import app.campfire.account.ui.picker.showAccountPicker
import app.campfire.account.ui.switcher.AccountSwitcher
import app.campfire.account.ui.switcher.AccountSwitcherUiEvent
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.AdaptiveCampfireLayout
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.util.withDensity
import app.campfire.common.navigator.HomeNavigator
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.DetailScreen
import app.campfire.common.screens.EmptyScreen
import app.campfire.common.screens.LoginScreen
import app.campfire.search.api.ui.LocalSearchEventHandler
import app.campfire.search.api.ui.SearchResultNavEvent
import app.campfire.search.api.ui.goToSearchEvent
import app.campfire.sessions.ui.PlaybackBar
import app.campfire.sessions.ui.PlaybackBottomBar
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.navigation.bar.CampfireNavigationBar
import app.campfire.ui.navigation.bar.LocalNavigationBarState
import app.campfire.ui.navigation.bar.rememberCampfireNavigationBarState
import app.campfire.ui.navigation.drawer.CampfireDrawer
import app.campfire.ui.navigation.rail.CampfireNavigationRail
import app.campfire.ui.theming.api.ThemeManager
import campfire.app.common.generated.resources.Res
import campfire.app.common.generated.resources.empty_supporting_pane_message
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import com.slack.circuitx.navigation.intercepting.NavigationEventListener
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RootUi(
  backstack: SaveableBackStack,
  navigator: Navigator,
  themeManager: ThemeManager,
  themeSettings: ThemeSettings,
  navigationEventListeners: ImmutableList<NavigationEventListener>,
  windowInsets: WindowInsets,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val windowSizeClass = LocalWindowSizeClass.current

  val rootScreen by remember(backstack) {
    derivedStateOf { backstack.last().screen }
  }

  val currentPresentation by remember(backstack) {
    derivedStateOf {
      (backstack.topRecord?.screen as? BaseScreen)?.presentation
    }
  }

  val detailBackStack = rememberSaveableBackStack(EmptyScreen(stringResource(Res.string.empty_supporting_pane_message)))
  val baseDetailNavigator = rememberCircuitNavigator(detailBackStack) { /* Do Nothing */ }
  val detailNavigator = rememberInterceptingNavigator(
    navigator = baseDetailNavigator,
    eventListeners = navigationEventListeners,
    enableBackHandler = false,
  )

  // If the user is switching between form factors, i.e. opening/closing a foldable
  // then we'll want to re-orientate the root and detail back stacks so the content
  // isn't rendered oddly.
  LaunchedEffect(windowSizeClass.isSupportingPaneEnabled) {
    if (windowSizeClass.isSupportingPaneEnabled) {
      val detailScreens = backstack.popUntil { it.screen !is DetailScreen }
      detailScreens.asReversed().forEach {
        detailBackStack.push(it)
      }
    } else {
      val detailScreens = detailBackStack.popUntil { it.screen is EmptyScreen }
      detailScreens.asReversed().forEach {
        backstack.push(it)
      }
    }
  }

  val detailRootScreen by remember(detailBackStack) {
    derivedStateOf { detailBackStack.topRecord?.screen }
  }

  val overlayHost = rememberOverlayHost()
  NavigationBackHandler(
    state = rememberNavigationEventState(NavigationEventInfo.None),
    isBackEnabled = overlayHost.currentOverlayData != null || detailRootScreen !is EmptyScreen,
    onBackCompleted = {
      overlayHost.currentOverlayData?.finish(Unit) ?: detailNavigator.pop()
    },
  )

  val homeNavigator = remember(navigator, windowSizeClass) {
    HomeNavigator(
      windowSizeClass = windowSizeClass,
      rootNavigator = navigator,
      detailNavigator = detailNavigator,
    )
  }

  val drawerState = rememberDrawerState(DrawerValue.Closed)
  LaunchedEffect(drawerState.currentValue) {
    if (drawerState.currentValue == DrawerValue.Open) {
      Analytics.send(ScreenViewEvent("Drawer", ScreenType.Overlay))
    }
  }

  var playbackBarExpanded by remember { mutableStateOf(false) }
  NavigationBackHandler(
    state = rememberNavigationEventState(NavigationEventInfo.None),
    isBackEnabled = playbackBarExpanded,
    onBackCompleted = {
      Analytics.send(ActionEvent("playback_bar", "collapsed", "back_handler"))
      playbackBarExpanded = false
    },
  )

  // Search View wiring
  val navigationBarState = rememberCampfireNavigationBarState()
  AdaptiveCampfireLayout(
    overlayHost = overlayHost,
    drawerState = drawerState,
    drawerEnabled = !playbackBarExpanded,
    windowInsets = windowInsets,
    hideBottomNav = currentPresentation?.hideBottomNav == true || playbackBarExpanded,

    drawerContent = {
      CampfireDrawer(
        rootScreen = rootScreen,
        drawerState = drawerState,
        navigator = homeNavigator,
        accountSwitcher = {
          AccountSwitcher(
            onClick = { eventSink ->
              coroutineScope.launch {
                async {
                  drawerState.close()
                }
                when (val result = overlayHost.showAccountPicker()) {
                  AccountPickerResult.AddAccount -> {
                    homeNavigator.goTo(LoginScreen.Additional)
                    drawerState.close()
                  }

                  is AccountPickerResult.SwitchAccount -> {
                    eventSink(AccountSwitcherUiEvent.SwitchAccount(result.server))
                    drawerState.close()
                  }

                  is AccountPickerResult.ReauthenticateAccount -> {
                    homeNavigator.goTo(LoginScreen.ReAuthentication(result.server))
                    drawerState.close()
                  }

                  else -> Unit
                }
              }
            },
          )
        },
      )
    },
    bottomBarNavigation = {
      CampfireNavigationBar(
        state = navigationBarState,
        selectedNavigation = rootScreen,
        onNavigationSelected = { homeNavigator.resetRoot(it) },
        modifier = Modifier.fillMaxWidth(),
      )
    },
    railNavigation = {
      CampfireNavigationRail(
        selectedNavigation = rootScreen,
        onNavigationSelected = { homeNavigator.resetRoot(it) },
        onMenuSelected = {
          coroutineScope.launch {
            drawerState.open()
          }
        },
        modifier = Modifier.fillMaxHeight(),
      )
    },

    content = {
      val searchEventHandler: (SearchResultNavEvent) -> Unit = remember(homeNavigator) {
        { event -> homeNavigator.goToSearchEvent(event) }
      }

      SharedElementTransitionLayout {
        CompositionLocalProvider(
          LocalSearchEventHandler provides searchEventHandler,
          LocalNavigationBarState provides navigationBarState,
        ) {
          NavigableCircuitContent(
            navigator = homeNavigator,
            backStack = backstack,
            decoratorFactory = remember(navigator) {
              GestureNavigationDecorationFactory(
                onBackInvoked = navigator::pop,
              )
            },
          )
        }
      }
    },
    playbackBarContent = {
      if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.ExtraLarge) {
        val bottomSystemInset = withDensity {
          WindowInsets.navigationBars.asPaddingValues()
            .calculateBottomPadding().toPx()
        }

        val bottomBarOffset = withDensity {
          bottomSystemInset + 8.dp.toPx()
        }

        PlaybackBar(
          enabled = currentPresentation?.hidePlaybackBar != true,
          expanded = playbackBarExpanded,
          onExpansionChange = {
            Analytics.send(ActionEvent("playback_bar", if (it) "expanded" else "collapsed"))
            playbackBarExpanded = it
          },
          navigator = homeNavigator,
          themeManager = themeManager,
          themeSettings = themeSettings,
          offset = {
            if (!windowSizeClass.isSupportingPaneEnabled && currentPresentation?.hideBottomNav != true) {
              val dy = navigationBarState.playbackBarOffset(bottomBarOffset).roundToInt()
              IntOffset(0, -dy)
            } else {
              IntOffset(0, -bottomSystemInset.fastRoundToInt())
            }
          },
          modifier = Modifier
            .align(Alignment.BottomStart)
            .widthIn(max = 500.dp)
            .fillMaxWidth(),
        )
      } else {
        PlaybackBottomBar(
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    showSupportingContent = detailRootScreen !is EmptyScreen,
    supportingContent = {
      SharedElementTransitionLayout {
        NavigableCircuitContent(
          navigator = detailNavigator,
          backStack = detailBackStack,
        )
      }
    },
    modifier = modifier,
  )
}
