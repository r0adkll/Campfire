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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.campfire.common.navigator.HomeNavigator
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.common.screens.DrawerScreen
import app.campfire.common.screens.EmptyScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.core.extensions.fluentIf
import app.campfire.core.logging.bark
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.search.ui.CampfireDockedSearchBar
import app.campfire.search.ui.showSearchOverlay
import app.campfire.sessions.ui.PlaybackBar
import app.campfire.sessions.ui.PlaybackBottomBar
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
import io.ktor.util.reflect.instanceOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeUi(
  backstack: SaveableBackStack,
  navigator: Navigator,
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
  val detailNavigator = rememberCircuitNavigator(detailBackStack) { /* Do Nothing */ }

  val detailRootScreen by remember(detailBackStack) {
    derivedStateOf { detailBackStack.topRecord?.screen }
  }

  val overlayHost = rememberOverlayHost()
  PlatformBackHandler(overlayHost.currentOverlayData != null || detailRootScreen !is EmptyScreen) {
    bark("OverlayHost") { "onBackHandler(${overlayHost.currentOverlayData})" }
    overlayHost.currentOverlayData?.finish(Unit) ?: detailNavigator.pop()
  }

  val homeNavigator = remember(navigator, windowSizeClass) {
    HomeNavigator(
      windowSizeClass = windowSizeClass,
      rootNavigator = navigator,
      detailNavigator = detailNavigator,
    )
  }

  val navigationItems = buildNavigationItems()
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  var playbackBarExpanded by remember { mutableStateOf(false) }
  PlatformBackHandler(playbackBarExpanded) { playbackBarExpanded = false }

  // Search View wiring
  AdaptiveCampfireLayout(
    overlayHost = overlayHost,
    drawerState = drawerState,
    drawerEnabled = !playbackBarExpanded,
    windowInsets = windowInsets,
    hideBottomNav = currentPresentation?.hideBottomNav == true || playbackBarExpanded,

    onSearchClick = {
      coroutineScope.launch {
        overlayHost.showSearchOverlay(homeNavigator)
      }
    },

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
      HomeNavigationBar(
        selectedNavigation = rootScreen,
        navigationItems = navigationItems,
        onNavigationSelected = { homeNavigator.resetRoot(it) },
        modifier = Modifier.fillMaxWidth(),
      )
    },
    railNavigation = {
      HomeNavigationRail(
        selectedNavigation = rootScreen,
        navigationItems = navigationItems,
        onNavigationSelected = { homeNavigator.resetRoot(it) },
        onMenuSelected = {
          coroutineScope.launch {
            drawerState.open()
          }
        },
        modifier = Modifier.fillMaxHeight(),
      )
    },

    searchBar = {
      CampfireDockedSearchBar(
        navigator = homeNavigator,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .fluentIf(currentPlatform == Platform.DESKTOP) {
            padding(top = 16.dp)
          },
      )
    },

    content = {
      SharedElementTransitionLayout {
        NavigableCircuitContent(
          navigator = homeNavigator,
          backStack = backstack,
          decoratorFactory = GestureNavigationDecorationFactory(
            onBackInvoked = navigator::pop,
          ),
        )
      }
    },
    playbackBarContent = {
      if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.ExtraLarge) {
        PlaybackBar(
          expanded = playbackBarExpanded,
          onExpansionChange = { playbackBarExpanded = it },
          navigator = homeNavigator,
          modifier = Modifier
            .align(Alignment.BottomStart)
            .widthIn(max = 500.dp)
            .fillMaxWidth()
            .fluentIf(windowSizeClass.isSupportingPaneEnabled) {
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

@Composable
private fun HomeNavigationBar(
  selectedNavigation: Screen,
  navigationItems: List<HomeNavigationItem>,
  onNavigationSelected: (Screen) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationBar(
    modifier = modifier,
    windowInsets = WindowInsets.navigationBars,
  ) {
    for (item in navigationItems) {
      NavigationBarItem(
        icon = {
          HomeNavigationItemIcon(
            item = item,
            selected = item.screen.instanceOf(selectedNavigation::class),
          )
        },
        label = { Text(text = item.label) },
        selected = selectedNavigation == item.screen,
        onClick = { onNavigationSelected(item.screen) },
      )
    }
  }
}

@Composable
private fun HomeNavigationRail(
  selectedNavigation: Screen,
  navigationItems: List<HomeNavigationItem>,
  onNavigationSelected: (Screen) -> Unit,
  onMenuSelected: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationRail(
    modifier = modifier,
    header = {
      IconButton(
        onClick = onMenuSelected,
      ) {
        Icon(Icons.Rounded.Menu, contentDescription = null)
      }
//      FloatingActionButton(
//        onClick = onCreateSelected,
//      ) {
//        Icon(
//          Icons.Rounded.Create,
//          contentDescription = null,
//        )
//      }
    },
  ) {
    for (item in navigationItems) {
      NavigationRailItem(
        icon = {
          HomeNavigationItemIcon(
            item = item,
            selected = item.screen.instanceOf(selectedNavigation::class),
          )
        },
        alwaysShowLabel = false,
        label = { Text(text = item.label) },
        selected = selectedNavigation == item.screen,
        onClick = { onNavigationSelected(item.screen) },
      )
    }

    Spacer(Modifier.weight(1f))
    NavigationRailItem(
      icon = {
        Icon(
          imageVector = Icons.Outlined.Settings,
          contentDescription = stringResource(Res.string.settings_content_description),
        )
      },
      label = { Text(text = stringResource(Res.string.settings)) },
      selected = false,
      onClick = { onNavigationSelected(SettingsScreen()) },
    )
  }
}

@Composable
private fun HomeNavigationItemIcon(item: HomeNavigationItem, selected: Boolean) {
  if (item.selectedImageVector != null) {
    Crossfade(targetState = selected) { s ->
      Icon(
        imageVector = if (s) item.selectedImageVector else item.iconImageVector,
        contentDescription = item.contentDescription,
      )
    }
  } else {
    Icon(
      imageVector = item.iconImageVector,
      contentDescription = item.contentDescription,
    )
  }
}

@Immutable
data class HomeNavigationItem(
  val screen: Screen,
  val label: String,
  val contentDescription: String,
  val iconImageVector: ImageVector,
  val selectedImageVector: ImageVector? = null,
)

// TODO: The central navigation items and such split between here (:shared) and other places (:ui:drawer) should
//  be unified and made consistent
@Composable
private fun buildNavigationItems(): List<HomeNavigationItem> {
  return listOf(
    HomeNavigationItem(
      screen = HomeScreen,
      label = stringResource(Res.string.nav_home_label),
      contentDescription = stringResource(Res.string.nav_home_content_description),
      iconImageVector = Icons.Outlined.Home,
      selectedImageVector = Icons.Filled.Home,
    ),
    HomeNavigationItem(
      screen = LibraryScreen(),
      label = stringResource(Res.string.nav_library_label),
      contentDescription = stringResource(Res.string.nav_library_content_description),
      iconImageVector = Icons.Outlined.Library,
      selectedImageVector = Icons.Filled.Library,
    ),
    HomeNavigationItem(
      screen = SeriesScreen,
      label = stringResource(Res.string.nav_series_label),
      contentDescription = stringResource(Res.string.nav_series_content_description),
      iconImageVector = Icons.Outlined.Series,
      selectedImageVector = Icons.Filled.Series,
    ),
    HomeNavigationItem(
      screen = CollectionsScreen,
      label = stringResource(Res.string.nav_collections_label),
      contentDescription = stringResource(Res.string.nav_collections_content_description),
      iconImageVector = Icons.Outlined.Collections,
      selectedImageVector = Icons.Filled.Collections,
    ),
    HomeNavigationItem(
      screen = AuthorsScreen,
      label = stringResource(Res.string.nav_authors_label),
      contentDescription = stringResource(Res.string.nav_authors_content_description),
      iconImageVector = Icons.Outlined.Author,
      selectedImageVector = Icons.Filled.Author,
    ),
  )
}
