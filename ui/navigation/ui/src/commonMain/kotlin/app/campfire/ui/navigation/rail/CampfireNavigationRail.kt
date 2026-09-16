// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.rail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Settings
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.reflect.instanceOf
import app.campfire.ui.navigation.HomeNavigationItem
import app.campfire.ui.navigation.HomeNavigationItemIcon
import app.campfire.ui.navigation.NavigationComponent
import campfire.ui.navigation.ui.generated.resources.Res
import campfire.ui.navigation.ui.generated.resources.settings
import campfire.ui.navigation.ui.generated.resources.settings_content_description
import com.slack.circuit.runtime.screen.Screen
import org.jetbrains.compose.resources.stringResource

private val ServerIconSizeSmall = 40.dp
private val ServerIconSizeLarge = 56.dp

@Composable
fun CampfireNavigationRail(
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  onMenuSelected: () -> Unit,
  modifier: Modifier = Modifier,
  navigationComponent: NavigationComponent = rememberComponent(),
) {
  val windowSizeClass = LocalWindowSizeClass.current
  val presenter = remember(navigationComponent) {
    navigationComponent.navigationPresenterFactory()
  }
  val navigationItems = presenter.present()

  CampfireNavigationRailContent(
    navigationItems = navigationItems,
    selectedNavigation = selectedNavigation,
    onNavigationSelected = onNavigationSelected,
    header = {
      ServerIcon(
        onClick = onMenuSelected,
        size = if (windowSizeClass.isLandscapePhone) ServerIconSizeSmall else ServerIconSizeLarge,
      )
    },
    modifier = modifier,
  )
}

/**
 * The compact rail, driven by plain values so it can be rendered in tests. The destinations
 * scroll when the rail is shorter than the list — a landscape phone, or the upper half of a
 * half-open foldable — while the header and the Settings item stay pinned at either end.
 */
@Composable
fun CampfireNavigationRailContent(
  navigationItems: List<HomeNavigationItem>,
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  header: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationRail(
    modifier = modifier,
    header = { header() },
  ) {
    // The rail neither scrolls nor wraps, so a long destination list scrolls inside one child.
    // Its weight takes whatever height the pinned Settings item leaves, which also keeps that
    // item at the bottom when the list is short.
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(ItemSpacing),
      horizontalAlignment = Alignment.CenterHorizontally,
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
    }

    NavigationRailItem(
      icon = {
        Icon(
          imageVector = CampfireIcons.Rounded.Settings,
          contentDescription = stringResource(Res.string.settings_content_description),
        )
      },
      label = { Text(text = stringResource(Res.string.settings)) },
      selected = false,
      onClick = { onNavigationSelected(SettingsScreen()) },
    )
  }
}

/** Mirrors the rail's own item spacing, which it only applies to direct children. */
private val ItemSpacing = 4.dp
