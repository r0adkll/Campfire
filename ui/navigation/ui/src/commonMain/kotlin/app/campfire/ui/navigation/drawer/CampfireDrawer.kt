// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.navigation.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Brightness6
import app.campfire.common.compose.icons.rounded.Cloud
import app.campfire.common.compose.icons.rounded.CloudOff
import app.campfire.common.compose.icons.rounded.DarkMode
import app.campfire.common.compose.icons.rounded.LightMode
import app.campfire.common.compose.icons.rounded.Palette
import app.campfire.common.compose.layout.NavigationType
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.layout.navigationType
import app.campfire.common.compose.offline.WorkOfflineMenu
import app.campfire.common.compose.offline.statusText
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.di.UserScope
import app.campfire.core.reflect.instanceOf
import app.campfire.network.reachability.OfflineChoice
import app.campfire.network.reachability.OfflineState
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.api.ThemeMode.DARK
import app.campfire.settings.api.ThemeMode.LIGHT
import app.campfire.settings.api.ThemeMode.SYSTEM
import app.campfire.ui.navigation.HomeNavigationItem
import app.campfire.ui.navigation.NavigationPresenterFactory
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.updates.AppUpdateWidget
import app.campfire.whatsnew.api.WhatsNewWidgetProvider
import app.campfire.whatsnew.api.screen.ChangelogScreen
import campfire.ui.navigation.ui.generated.resources.Res
import campfire.ui.navigation.ui.generated.resources.action_change_theme
import campfire.ui.navigation.ui.generated.resources.action_change_theme_mode
import campfire.ui.navigation.ui.generated.resources.action_work_offline
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface CampfireDrawerComponent {
  val whatsNewWidget: WhatsNewWidgetProvider
  val appUpdateWidget: AppUpdateWidget
  val navigationPresenterFactory: NavigationPresenterFactory
}

@Composable
fun CampfireDrawer(
  currentScreen: Screen,
  drawerState: DrawerState,
  navigator: Navigator,
  accountSwitcher: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  component: CampfireDrawerComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()
  val presenter = remember(component) {
    component.navigationPresenterFactory()
  }
  val state = presenter.presentDrawer()
  val windowSizeClass = LocalWindowSizeClass.current
  val isPermanentDrawer = windowSizeClass.navigationType == NavigationType.Drawer

  // Prefer an exact match so sibling items sharing a screen class (e.g. Settings and
  // Downloads) don't both light up; fall back to the class for parameterised screens.
  val selectedScreen = state.navigationItems.firstOrNull { it.screen == currentScreen }?.screen
    ?: state.navigationItems.firstOrNull { it.screen.instanceOf(currentScreen::class) }?.screen

  DrawerSheet(
    modifier = modifier,
    windowInsets = if (windowSizeClass.isLandscapePhone) {
      DrawerDefaults.windowInsets
        .only(WindowInsetsSides.Horizontal)
    } else {
      DrawerDefaults.windowInsets
    },
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
    ) {
      if (windowSizeClass.isLandscapePhone) {
        Spacer(Modifier.height(24.dp))
      }

      accountSwitcher()

      component.whatsNewWidget.Content(
        onClick = {
          navigator.goTo(ChangelogScreen)
          scope.launch {
            drawerState.close()
          }
        },
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )

      component.appUpdateWidget.Content(
        onClick = {
          navigator.goTo(SettingsScreen(SettingsScreen.Page.Updates))
          scope.launch {
            drawerState.close()
          }
        },
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )

      Spacer(Modifier.height(8.dp))

      state.navigationItems.forEach { item ->
        DestinationListItem(
          item = item,
          selected = item.screen == selectedScreen,
          onClick = {
            if (isPermanentDrawer) {
              // A permanent drawer is the top-level switcher, like the bottom bar and rail:
              // swap the root instead of stacking destinations on top of each other.
              navigator.resetRoot(item.screen)
            } else {
              navigator.goTo(item.screen)
              scope.launch {
                drawerState.close()
              }
            }
          },
        )
      }
    }

    DrawerActions(
      themeMode = state.themeMode,
      offline = state.offline,
      canDetectWifi = state.canDetectWifi,
      onGoOffline = { state.eventSink(DrawerUiEvent.GoOffline(it)) },
      onGoOnline = { state.eventSink(DrawerUiEvent.GoOnline) },
      onCycleThemeMode = { state.eventSink(DrawerUiEvent.CycleThemeMode) },
      onChangeTheme = {
        navigator.goTo(ThemePickerScreen)
        scope.launch {
          drawerState.close()
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          horizontal = 16.dp,
          vertical = 8.dp,
        ),
    )
  }
}

/**
 * The drawer's footer: the work-offline button, which opens the [WorkOfflineMenu], then the theme
 * mode and theme picker pair.
 */
@Composable
internal fun DrawerActions(
  themeMode: ThemeMode,
  offline: OfflineState,
  canDetectWifi: Boolean,
  onGoOffline: (OfflineChoice) -> Unit,
  onGoOnline: () -> Unit,
  onCycleThemeMode: () -> Unit,
  onChangeTheme: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
  ) {
    var menuExpanded by remember { mutableStateOf(false) }
    val workOfflineLabel = offline.reason?.statusText() ?: stringResource(Res.string.action_work_offline)
    Box {
      IconButtonTooltip(
        text = workOfflineLabel,
      ) {
        OutlinedIconToggleButton(
          checked = offline.isOffline,
          onCheckedChange = { menuExpanded = true },
          // Round like the theme pair's outer ends, squaring off while working offline so the
          // state reads from the shape as well as the color
          shapes = IconButtonDefaults.toggleableShapes(),
          // Outlined in the content color like the theme mode button beside it
          colors = IconButtonDefaults.outlinedIconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          ),
          border = IconButtonDefaults.outlinedIconToggleButtonBorder(enabled = true, checked = offline.isOffline),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(
              IconButtonDefaults.smallContainerSize(
                IconButtonDefaults.IconButtonWidthOption.Wide,
              ),
            ),
        ) {
          // A plain cloud while connected, crossed out while working offline
          AnimatedContent(
            targetState = offline.isOffline,
            transitionSpec = { iconSwap() },
          ) { isOffline ->
            Icon(
              if (isOffline) CampfireIcons.Rounded.CloudOff else CampfireIcons.Rounded.Cloud,
              contentDescription = workOfflineLabel,
            )
          }
        }
      }

      WorkOfflineMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        state = offline,
        canDetectWifi = canDetectWifi,
        onGoOffline = onGoOffline,
        onGoOnline = onGoOnline,
      )
    }

    Spacer(Modifier.weight(1f))

    val changeThemeModeLabel = stringResource(Res.string.action_change_theme_mode)
    IconButtonTooltip(
      text = changeThemeModeLabel,
    ) {
      OutlinedIconButton(
        shapes = IconButtonDefaults.shapes(
          shape = RoundedCornerShape(
            topStart = CornerSize(50),
            bottomStart = CornerSize(50),
            topEnd = CornerSize(4.dp),
            bottomEnd = CornerSize(4.dp),
          ),
        ),
        onClick = onCycleThemeMode,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .size(
            IconButtonDefaults.smallContainerSize(
              IconButtonDefaults.IconButtonWidthOption.Wide,
            ),
          ),
      ) {
        AnimatedContent(
          targetState = themeMode,
          transitionSpec = { iconSwap() },
        ) { mode ->
          Icon(
            when (mode) {
              LIGHT -> CampfireIcons.Rounded.LightMode
              DARK -> CampfireIcons.Rounded.DarkMode
              SYSTEM -> CampfireIcons.Rounded.Brightness6
            },
            contentDescription = changeThemeModeLabel,
          )
        }
      }
    }

    val changeThemeLabel = stringResource(Res.string.action_change_theme)
    IconButtonTooltip(
      text = changeThemeLabel,
    ) {
      FilledTonalIconButton(
        shapes = IconButtonDefaults.shapes(
          shape = RoundedCornerShape(
            topStart = CornerSize(4.dp),
            bottomStart = CornerSize(4.dp),
            topEnd = CornerSize(50),
            bottomEnd = CornerSize(50),
          ),
        ),
        colors = IconButtonDefaults.filledTonalIconButtonColors(),
        onClick = onChangeTheme,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .size(
            IconButtonDefaults.smallContainerSize(
              IconButtonDefaults.IconButtonWidthOption.Wide,
            ),
          ),
      ) {
        Icon(CampfireIcons.Rounded.Palette, contentDescription = changeThemeLabel)
      }
    }
  }
}

/** The footer buttons' icon change: the old icon shrinks away, then the new one grows in. */
private fun <S> AnimatedContentTransitionScope<S>.iconSwap(): ContentTransform {
  val inSpec: FiniteAnimationSpec<Float> = tween(220, delayMillis = 90)
  return (
    fadeIn(animationSpec = inSpec) + scaleIn(
      initialScale = 0f,
      animationSpec = inSpec,
    )
    ) togetherWith (
    scaleOut(
      animationSpec = tween(90),
    ) + fadeOut(tween(90))
    )
}

@Composable
private fun DestinationListItem(
  item: HomeNavigationItem,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationDrawerItem(
    icon = {
      Icon(
        imageVector = item.iconImageVector,
        contentDescription = item.contentDescription,
      )
    },
    label = { Text(text = item.label) },
    badge = { if (item.badged) Badge() },
    selected = selected,
    onClick = onClick,
    modifier = modifier
      .padding(
        horizontal = 16.dp,
      ),
  )
}

@Composable
private fun DrawerSheet(
  modifier: Modifier = Modifier,
  windowInsets: WindowInsets = DrawerDefaults.windowInsets,
  content: @Composable ColumnScope.() -> Unit,
) {
  val navigationType = LocalWindowSizeClass.current.navigationType
  if (navigationType == NavigationType.Drawer) {
    PermanentDrawerSheet(
      content = content,
      modifier = modifier,
      windowInsets = windowInsets,
    )
  } else {
    ModalDrawerSheet(
      content = content,
      modifier = modifier,
      windowInsets = windowInsets,
    )
  }
}
