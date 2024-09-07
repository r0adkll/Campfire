package app.campfire.shared.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import app.campfire.common.compose.navigation.LocalDrawerState
import app.campfire.common.compose.navigation.LocalRootScreen
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.screens.AuthorsScreen
import app.campfire.common.screens.BaseScreen
import app.campfire.common.screens.CollectionsScreen
import app.campfire.common.screens.DrawerScreen
import app.campfire.common.screens.EmptyScreen
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.LibraryScreen
import app.campfire.common.screens.SeriesScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.extensions.fluentIf
import app.campfire.core.logging.bark
import app.campfire.shared.navigator.HomeNavigator
import app.campfire.shared.navigator.NavigationType
import app.campfire.shared.navigator.isSupportingPaneEnabled
import app.campfire.shared.navigator.navigationType
import campfire.shared.generated.resources.Res
import campfire.shared.generated.resources.empty_supporting_pane_message
import campfire.shared.generated.resources.nav_authors_content_description
import campfire.shared.generated.resources.nav_authors_label
import campfire.shared.generated.resources.nav_collections_content_description
import campfire.shared.generated.resources.nav_collections_label
import campfire.shared.generated.resources.nav_home_content_description
import campfire.shared.generated.resources.nav_home_label
import campfire.shared.generated.resources.nav_library_content_description
import campfire.shared.generated.resources.nav_library_label
import campfire.shared.generated.resources.nav_series_content_description
import campfire.shared.generated.resources.nav_series_label
import campfire.shared.generated.resources.settings
import campfire.shared.generated.resources.settings_content_description
import com.moriatsushi.insetsx.navigationBars
import com.moriatsushi.insetsx.safeContentPadding
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.gesturenavigation.GestureNavigationDecoration
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Home(
  backstack: SaveableBackStack,
  navigator: Navigator,
  windowInsets: WindowInsets,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()

  val windowSizeClass = LocalWindowSizeClass.current
  val navigationType = remember(windowSizeClass) {
    windowSizeClass.navigationType
  }
  val isSupportingPaneEnabled = remember(windowSizeClass) {
    windowSizeClass.isSupportingPaneEnabled
  }

  val rootScreen by remember(backstack) {
    derivedStateOf { backstack.last().screen }
  }

  val currentPresentation by remember(backstack) {
    derivedStateOf {
      (backstack.topRecord?.screen as? BaseScreen)?.presentation
    }
  }

  val navigationItems = buildNavigationItems()

  val overlayHost = rememberOverlayHost()
  PlatformBackHandler(overlayHost.currentOverlayData != null) {
    overlayHost.currentOverlayData?.finish(Unit)
  }

  /*
   * Detail navigation setup
   */

  val detailBackStack = rememberSaveableBackStack(EmptyScreen(stringResource(Res.string.empty_supporting_pane_message)))
  val detailNavigator = rememberCircuitNavigator(detailBackStack) { /* Do Nothing */ }

  val homeNavigator = remember(windowSizeClass) {
    HomeNavigator(
      windowSizeClass = windowSizeClass,
      rootNavigator = navigator,
      detailNavigator = detailNavigator,
    )
  }

  ContentWithOverlays(
    overlayHost = overlayHost,
  ) {

    // This wraps a ModalNavigationDrawer IF the navigationType is Rail or BottomNav
    // otherwise, this just pass the content() block through
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    DrawerWithContent(
      navigationType = navigationType,
      drawerState = drawerState,
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
    ) {
      Scaffold(
        bottomBar = {
          if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
            AnimatedVisibility(
              visible = currentPresentation?.hideBottomNav == false,
              enter = slideInVertically { it },
              exit = slideOutVertically { it },
            ) {
              HomeNavigationBar(
                selectedNavigation = rootScreen,
                navigationItems = navigationItems,
                onNavigationSelected = { homeNavigator.resetRoot(it) },
                modifier = Modifier.fillMaxWidth(),
              )
            }
          } else {
            Spacer(
              Modifier
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .fillMaxWidth(),
            )
          }
        },
        // We let content handle the status bar
        contentWindowInsets = windowInsets,
        modifier = modifier,
      ) { paddingValues ->

        Row(
          modifier = Modifier
            .fillMaxSize()
            .fluentIf(
              navigationType == NavigationType.BOTTOM_NAVIGATION &&
                currentPresentation?.hideBottomNav != true,
            ) {
              padding(paddingValues)
            },
        ) {
          if (navigationType == NavigationType.RAIL) {
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
          }

          NavigableCircuitContent(
            navigator = homeNavigator,
            backStack = backstack,
            decoration = GestureNavigationDecoration(
              onBackInvoked = navigator::pop,
            ),
            modifier = modifier.weight(1f),
          )

          if (isSupportingPaneEnabled) {
            Surface(
              modifier = Modifier.width(360.dp),
              shadowElevation = 6.dp,
              tonalElevation = 6.dp,
              shape = RoundedCornerShape(
                topStart = 32.dp,
                bottomStart = 32.dp,
              )
            ) {
              NavigableCircuitContent(
                navigator = detailNavigator,
                backStack = detailBackStack,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DrawerWithContent(
  navigationType: NavigationType,
  modifier: Modifier = Modifier,
  drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
  drawerContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  // TODO: These are the only states now so this is effectively always true. We should evaluate whether
  //  or not this is still needed and if this should be the default
  if (navigationType == NavigationType.BOTTOM_NAVIGATION || navigationType == NavigationType.RAIL) {
    CompositionLocalProvider(
      LocalDrawerState provides drawerState,
    ) {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        modifier = modifier,
      ) {
        content()
      }
    }
  } else {
    content()
  }
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
            selected = selectedNavigation == item.screen,
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
        onClick = onMenuSelected
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
            selected = selectedNavigation == item.screen,
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
      onClick = { onNavigationSelected(SettingsScreen) },
    )
  }
}

@Composable
private fun HomeNavigationDrawer(
  selectedNavigation: Screen,
  navigationItems: List<HomeNavigationItem>,
  onNavigationSelected: (Screen) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .safeContentPadding()
      .padding(16.dp)
      .widthIn(max = 280.dp),
  ) {
    for (item in navigationItems) {
      @OptIn(ExperimentalMaterial3Api::class)
      NavigationDrawerItem(
        icon = {
          Icon(
            imageVector = item.iconImageVector,
            contentDescription = item.contentDescription,
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
      screen = LibraryScreen,
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
