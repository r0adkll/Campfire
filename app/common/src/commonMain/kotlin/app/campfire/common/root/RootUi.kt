@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.campfire.common.root

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.PlatformBackHandler
import app.campfire.common.compose.icons.filled.Author
import app.campfire.common.compose.icons.filled.Collections
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.compose.icons.filled.Library
import app.campfire.common.compose.icons.filled.Series
import app.campfire.common.compose.icons.outline.Author
import app.campfire.common.compose.icons.outline.Collections
import app.campfire.common.compose.icons.outline.Home
import app.campfire.common.compose.icons.outline.Library
import app.campfire.common.compose.icons.outline.Series
import app.campfire.common.compose.layout.AdaptiveCampfireLayout
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.navigation.LocalRootScreen
import app.campfire.common.compose.widgets.AppBarState
import app.campfire.common.compose.widgets.ServerIcon
import app.campfire.common.compose.widgets.ServerState
import app.campfire.common.navigator.HomeNavigator
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.common.screens.DetailScreen
import app.campfire.common.screens.DrawerScreen
import app.campfire.common.screens.EmptyScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Server
import app.campfire.core.model.Tent
import app.campfire.core.model.User
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.search.api.ui.LocalSearchEventHandler
import app.campfire.search.api.ui.SearchResultNavEvent
import app.campfire.search.api.ui.goToSearchEvent
import app.campfire.sessions.ui.PlaybackBar
import app.campfire.sessions.ui.PlaybackBottomBar
import app.campfire.ui.navigation.bar.CampfireNavigationBar
import app.campfire.ui.navigation.rail.CampfireNavigationRail
import app.campfire.ui.theming.api.ThemeManager
import campfire.app.common.generated.resources.Res
import campfire.app.common.generated.resources.empty_supporting_pane_message
import campfire.app.common.generated.resources.nav_authors_content_description
import campfire.app.common.generated.resources.nav_authors_label
import campfire.app.common.generated.resources.nav_collections_content_description
import campfire.app.common.generated.resources.nav_collections_label
import campfire.app.common.generated.resources.nav_home_content_description
import campfire.app.common.generated.resources.nav_home_label
import campfire.app.common.generated.resources.nav_library_content_description
import campfire.app.common.generated.resources.nav_library_label
import campfire.app.common.generated.resources.nav_series_content_description
import campfire.app.common.generated.resources.nav_series_label
import campfire.app.common.generated.resources.settings
import campfire.app.common.generated.resources.settings_content_description
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.sharedelements.SharedElementTransitionLayout
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import com.slack.circuitx.navigation.intercepting.NavigationEventListener
import com.slack.circuitx.navigation.intercepting.rememberInterceptingNavigator
import io.ktor.util.reflect.instanceOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RootUi(
  backstack: SaveableBackStack,
  navigator: Navigator,
  themeManager: ThemeManager,
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
  PlatformBackHandler(overlayHost.currentOverlayData != null || detailRootScreen !is EmptyScreen) {
    overlayHost.currentOverlayData?.finish(Unit) ?: detailNavigator.pop()
  }

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
  PlatformBackHandler(playbackBarExpanded) {
    Analytics.send(ActionEvent("playback_bar", "collapsed", "back_handler"))
    playbackBarExpanded = false
  }

  // Search View wiring
  AdaptiveCampfireLayout(
    overlayHost = overlayHost,
    drawerState = drawerState,
    drawerEnabled = !playbackBarExpanded,
    windowInsets = windowInsets,
    hideBottomNav = currentPresentation?.hideBottomNav == true || playbackBarExpanded,

    drawerContent = {
      CompositionLocalProvider(
        LocalRootScreen provides rootScreen,
      ) {
        CircuitContent(
          screen = DrawerScreen,
          navigator = homeNavigator,
        )
      }
    },
    bottomBarNavigation = {
      CampfireNavigationBar(
        selectedNavigation = rootScreen,
        onNavigationSelected = { homeNavigator.resetRoot(it) },
        modifier = Modifier.fillMaxWidth()
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
      val searchEventHandler: (SearchResultNavEvent) -> Unit = remember {
        { event -> homeNavigator.goToSearchEvent(event) }
      }

      SharedElementTransitionLayout {
        CompositionLocalProvider(
          LocalSearchEventHandler provides searchEventHandler,
        ) {
          NavigableCircuitContent(
            navigator = homeNavigator,
            backStack = backstack,
            decoratorFactory = GestureNavigationDecorationFactory(
              onBackInvoked = navigator::pop,
            ),
          )
        }
      }
    },
    playbackBarContent = {
      if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.ExtraLarge) {
        PlaybackBar(
          expanded = playbackBarExpanded,
          onExpansionChange = {
            Analytics.send(ActionEvent("playback_bar", if (it) "expanded" else "collapsed"))
            playbackBarExpanded = it
          },
          navigator = homeNavigator,
          themeManager = themeManager,
          modifier = Modifier
            .align(Alignment.BottomStart)
            .widthIn(max = 500.dp)
            .fillMaxWidth()
            .fluentIf(windowSizeClass.isSupportingPaneEnabled || currentPresentation?.hideBottomNav == true) {
              navigationBarsPadding()
            },
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
