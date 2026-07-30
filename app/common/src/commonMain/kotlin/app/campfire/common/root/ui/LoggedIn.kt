// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.campfire.common.root.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import app.campfire.account.ui.picker.AccountPickerResult
import app.campfire.account.ui.picker.showAccountPicker
import app.campfire.account.ui.switcher.AccountSwitcher
import app.campfire.account.ui.switcher.AccountSwitcherUiEvent
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.back.OverlayPriorityBackHandler
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.shouldUseDarkColors
import app.campfire.common.compose.layout.AdaptiveCampfireLayout
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.session.LocalPlaybackSession
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.util.LocalThemeDispatcher
import app.campfire.common.compose.util.ThemeDispatcher
import app.campfire.common.compose.util.withDensity
import app.campfire.common.compose.widgets.LocalItemCardMarquee
import app.campfire.common.di.UserComponent
import app.campfire.common.navigator.HomeNavigator
import app.campfire.common.navigator.OpenUrlNavigator
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.DetailScreen
import app.campfire.common.screens.EmptyScreen
import app.campfire.common.screens.LoginScreen
import app.campfire.core.logging.bark
import app.campfire.core.navigation.DeepLink
import app.campfire.core.session.requiredUserId
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.search.api.ui.LocalSearchEventHandler
import app.campfire.search.api.ui.SearchResultNavEvent
import app.campfire.search.api.ui.goToSearchEvent
import app.campfire.sessions.ui.PlaybackBottomBar
import app.campfire.sessions.ui.playback.CampfirePlaybackBar
import app.campfire.settings.api.CampfireSettings
import app.campfire.ui.navigation.bar.CampfireNavigationBar
import app.campfire.ui.navigation.bar.LocalNavigationBarState
import app.campfire.ui.navigation.bar.rememberCampfireNavigationBarState
import app.campfire.ui.navigation.drawer.CampfireDrawer
import app.campfire.ui.navigation.rail.CampfireNavigationRail
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.ui.theming.api.colorScheme
import campfire.app.common.generated.resources.Res
import campfire.app.common.generated.resources.empty_supporting_pane_message
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.retained.rememberRetainedSaveable
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import com.slack.circuitx.navigation.intercepting.NavigationEventListener
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoggedInWindow(
  userComponent: UserComponent,
  onRootPop: () -> Unit,
  onOpenUrl: (String) -> Unit,
  windowInsets: WindowInsets,
  deepLink: DeepLink,
  settings: CampfireSettings,
  themeManager: ThemeManager,
  themeRepository: AppThemeRepository,
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
      // Observe here and wire as composition local to avoid N-number of parameter
      // burials to wire all usages of this component
      val itemCardMarqueeEnabled by remember {
        settings.observeLibraryItemMarqueeEnabled()
      }.collectAsState()

      CompositionLocalProvider(
        LocalPlaybackSession provides currentSession,
        LocalThemeDispatcher provides themeManagerDispatcher,
        LocalItemCardMarquee provides itemCardMarqueeEnabled,
      ) {
        // The entire Compose hierarchy under this should be keyed and
        // unique per-user.
        key(userComponent.currentUserSession.requiredUserId) {
          LoggedInUi(
            backstack = backStack,
            navigator = urlNavigator,
            windowInsets = windowInsets,
            navigationEventListeners = userComponent.navigationEventListeners,
            deepLink = deepLink,
            modifier = modifier,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoggedInUi(
  backstack: SaveableBackStack,
  navigator: Navigator,
  navigationEventListeners: ImmutableList<NavigationEventListener>,
  windowInsets: WindowInsets,
  deepLink: DeepLink,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val windowSizeClass = LocalWindowSizeClass.current

  LaunchedEffect(windowSizeClass) {
    bark {
      "WindowSizeClass[${windowSizeClass.widthSizeClass}, ${windowSizeClass.heightSizeClass}]"
    }
  }

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
  // FIXME: Handle deep links in coordination with this bit
  LaunchedEffect(windowSizeClass.isSupportingPaneEnabled, deepLink) {
    if (windowSizeClass.isSupportingPaneEnabled) {
      val detailScreens = backstack.popUntil { it.screen !is DetailScreen }
      detailScreens.asReversed().forEach {
        detailBackStack.push(it)
      }

      when (deepLink) {
        is DeepLink.ItemDetail -> {
          detailBackStack.push(LibraryItemScreen(deepLink.libraryItemId))
        }
        DeepLink.None -> Unit
      }
    } else {
      val detailScreens = detailBackStack.popUntil { it.screen is EmptyScreen }
      detailScreens.asReversed().forEach {
        backstack.push(it)
      }

      when (deepLink) {
        is DeepLink.ItemDetail -> {
          backstack.push(LibraryItemScreen(deepLink.libraryItemId))
        }
        DeepLink.None -> Unit
      }
    }
  }

  val detailRootScreen by remember(detailBackStack) {
    derivedStateOf { detailBackStack.topRecord?.screen }
  }

  val overlayHost = rememberOverlayHost()

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

  var playbackBarExpanded by rememberRetainedSaveable { mutableStateOf(false) }

  // A single, deterministic back handler for all "chrome" back consumers. The order of the
  // `when` below *is* the priority — read top to bottom. Registered at PRIORITY_OVERLAY (via
  // OverlayPriorityBackHandler) so it always takes precedence over Circuit's main back stack
  // pop, regardless of composition order. Circuit still animates the pop when nothing here is
  // active.
  OverlayPriorityBackHandler(
    enabled = overlayHost.currentOverlayData != null ||
      playbackBarExpanded ||
      detailRootScreen !is EmptyScreen,
    onBack = {
      when {
        overlayHost.currentOverlayData != null -> overlayHost.currentOverlayData?.finish(Unit)

        playbackBarExpanded -> {
          Analytics.send(ActionEvent("playback_bar", "collapsed", "back_handler"))
          playbackBarExpanded = false
        }

        detailRootScreen !is EmptyScreen -> detailNavigator.pop()
      }
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
                launch {
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
      val shouldHideNavBar = currentPresentation?.hideBottomNav == true || playbackBarExpanded

      LaunchedEffect(shouldHideNavBar) {
        with(navigationBarState) {
          updateShouldHide(shouldHideNavBar)
        }
      }

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

        CampfirePlaybackBar(
          enabled = currentPresentation?.hidePlaybackBar != true,
          expanded = playbackBarExpanded,
          onExpansionChange = {
            Analytics.send(ActionEvent("playback_bar", if (it) "expanded" else "collapsed"))
            playbackBarExpanded = it
          },
          navigator = homeNavigator,
          offset = {
            if (!windowSizeClass.isSupportingPaneEnabled) {
              val dy = navigationBarState.playbackBarOffset().roundToInt()
              IntOffset(0, -dy)
            } else {
              IntOffset(0, -bottomSystemInset.fastRoundToInt())
            }
          },
          modifier = Modifier
            .align(Alignment.BottomStart)
            .widthIn(
              max = if (windowSizeClass.isLandscapePhone) {
                700.dp
              } else {
                500.dp
              },
            )
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
