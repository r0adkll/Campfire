// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.rail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.di.rememberComponent
import app.campfire.ui.navigation.HomeNavigationItemIcon
import app.campfire.ui.navigation.NavigationComponent
import campfire.ui.navigation.ui.generated.resources.Res
import campfire.ui.navigation.ui.generated.resources.action_collapse_navigation
import campfire.ui.navigation.ui.generated.resources.action_expand_navigation
import com.slack.circuit.runtime.screen.Screen
import org.jetbrains.compose.resources.stringResource

/**
 * What the rail header slot is given: whether the rail is expanded, the accessible label for the
 * control that toggles it, and the toggle itself. The slot content is expected to make its account
 * icon call [onToggle], so the icon is the rail's expand/collapse control.
 */
@Stable
data class WideNavigationRailHeaderScope(
  val expanded: Boolean,
  val toggleLabel: String,
  val onToggle: () -> Unit,
)

/**
 * The collapsible wide navigation rail used on desktop windows between the Expanded and
 * Extra-Large breakpoints. It lists every destination the permanent drawer would, and its header
 * hosts the account switcher, whose icon expands and collapses the rail.
 */
@Composable
fun CampfireWideNavigationRail(
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  accountContent: @Composable WideNavigationRailHeaderScope.() -> Unit,
  modifier: Modifier = Modifier,
  navigationComponent: NavigationComponent = rememberComponent(),
) {
  val presenter = remember(navigationComponent) {
    navigationComponent.navigationPresenterFactory()
  }
  val state = presenter.presentWideRail()

  CampfireWideNavigationRailContent(
    state = state,
    selectedNavigation = selectedNavigation,
    onNavigationSelected = onNavigationSelected,
    accountContent = accountContent,
    modifier = modifier,
  )
}

@Composable
fun CampfireWideNavigationRailContent(
  state: WideNavigationRailUiState,
  selectedNavigation: Screen,
  onNavigationSelected: (Screen) -> Unit,
  accountContent: @Composable WideNavigationRailHeaderScope.() -> Unit,
  modifier: Modifier = Modifier,
) {
  val railState = rememberWideNavigationRailState(
    initialValue = if (state.expanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed,
  )
  LaunchedEffect(state.expanded) {
    if (state.expanded) railState.expand() else railState.collapse()
  }
  val expanded = railState.targetValue == WideNavigationRailValue.Expanded

  // Mirror the rail's own item spacing, which it only applies to direct children
  val itemSpacing by animateDpAsState(if (expanded) 0.dp else CollapsedItemSpacing)
  // Centre the collapsed icon in the collapsed rail; hug the start edge once expanded
  val headerStartPadding by animateDpAsState(if (expanded) ExpandedHeaderPadding else CollapsedHeaderPadding)

  val toggleLabel = stringResource(
    if (expanded) Res.string.action_collapse_navigation else Res.string.action_expand_navigation,
  )
  val headerScope = remember(expanded, toggleLabel, state.eventSink) {
    WideNavigationRailHeaderScope(
      expanded = expanded,
      toggleLabel = toggleLabel,
      onToggle = { state.eventSink(WideNavigationRailUiEvent.ToggleExpanded) },
    )
  }

  WideNavigationRail(
    state = railState,
    modifier = modifier,
    header = {
      Box(Modifier.padding(start = headerStartPadding, end = ExpandedHeaderPadding)) {
        headerScope.accountContent()
      }
    },
  ) {
    // The rail neither scrolls nor wraps, so a long destination list scrolls inside one child
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
      for (item in state.navigationItems) {
        val selected = item.screen == selectedNavigation
        WideNavigationRailItem(
          selected = selected,
          onClick = { onNavigationSelected(item.screen) },
          icon = {
            HomeNavigationItemIcon(
              item = item,
              selected = selected,
            )
          },
          label = { Text(text = item.label) },
          railExpanded = expanded,
        )
      }
    }
  }
}

private val CollapsedItemSpacing = 4.dp

/** Centres a 56dp icon inside the 96dp collapsed rail. */
private val CollapsedHeaderPadding = 20.dp

/** Insets the expanded switcher card inside the 220dp minimum expanded rail. */
private val ExpandedHeaderPadding = 12.dp
