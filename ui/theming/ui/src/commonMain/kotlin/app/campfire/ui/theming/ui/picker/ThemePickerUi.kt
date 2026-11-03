// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Brightness6
import app.campfire.common.compose.icons.rounded.DarkMode
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.LightMode
import app.campfire.common.compose.icons.theme.Ai
import app.campfire.common.compose.icons.theme.rememberWallVectorPainter
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.ui.theming.ui.layout.PreviewLayout
import app.campfire.ui.theming.ui.layout.PreviewLayoutBox
import app.campfire.ui.theming.ui.layout.SidePreviewEndPadding
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_name_dynamic
import campfire.ui.theming.ui.generated.resources.theme_name_forest
import campfire.ui.theming.ui.generated.resources.theme_name_life_float
import campfire.ui.theming.ui.generated.resources.theme_name_mountain
import campfire.ui.theming.ui.generated.resources.theme_name_rucksack
import campfire.ui.theming.ui.generated.resources.theme_name_tent
import campfire.ui.theming.ui.generated.resources.theme_name_water_bottle
import campfire.ui.theming.ui.generated.resources.themes_mode_dark
import campfire.ui.theming.ui.generated.resources.themes_mode_light
import campfire.ui.theming.ui.generated.resources.themes_mode_system
import campfire.ui.theming.ui.generated.resources.themes_title
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

/**
 * Picks the app's theme, applying it straight away so the screen itself recolors, with the light
 * and dark setting beside the themes. Phones get a gallery of every theme drawn as a small copy
 * of Home ([ThemeGallery]). Foldables and tablets get a list of the themes beside a large
 * preview of the one in use ([ThemeList], [ThemeDetail]), split on the hinge when the device is
 * half-open.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(ThemePickerScreen::class, UserScope::class)
@Composable
fun ThemePicker(
  state: ThemePickerUiState,
  modifier: Modifier = Modifier,
) {
  MaterialExpressiveTheme(
    colorScheme = colorScheme(state.currentTheme),
  ) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
      topBar = {
        CampfireTopAppBar(
          title = { Text(stringResource(Res.string.themes_title)) },
          navigationIcon = {
            NavigationBackButton(onClick = { state.eventSink(ThemePickerUiEvent.Back) })
          },
          scrollBehavior = scrollBehavior,
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
      },
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      contentWindowInsets = CampfireWindowInsets,
    ) { paddingValues ->
      PreviewLayoutBox(contentPadding = paddingValues, modifier = Modifier.fillMaxSize()) { layout ->
        ThemePickerContent(state = state, layout = layout, contentPadding = paddingValues)
      }
    }
  }
}

/** The picker's themes laid out for [layout], inside the screen's [contentPadding]. */
@Composable
internal fun ThemePickerContent(
  state: ThemePickerUiState,
  layout: PreviewLayout,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  // The theme waiting on the user to confirm its deletion, by id so it survives recreation.
  var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
  val pendingDelete = (state.customThemes as? LoadState.Loaded)?.data?.find { it.id == pendingDeleteId }
  if (pendingDelete != null) {
    DeleteThemeDialog(
      theme = pendingDelete,
      isCurrent = pendingDelete.key == state.currentTheme.key,
      onConfirm = {
        pendingDeleteId = null
        state.eventSink(ThemePickerUiEvent.Delete(pendingDelete))
      },
      onDismiss = { pendingDeleteId = null },
    )
  }
  val onDeleteClick: (AppTheme.Fixed) -> Unit = { pendingDeleteId = it.id }

  val layoutDirection = LocalLayoutDirection.current
  val start = contentPadding.calculateStartPadding(layoutDirection)
  val end = contentPadding.calculateEndPadding(layoutDirection)
  val top = contentPadding.calculateTopPadding()
  val bottom = contentPadding.calculateBottomPadding()
  when (layout) {
    PreviewLayout.Compact -> ThemeGallery(
      state = state,
      onDeleteClick = onDeleteClick,
      contentPadding = contentPadding,
      modifier = modifier.fillMaxSize(),
    )

    is PreviewLayout.Tabletop -> Column(
      modifier = modifier
        .fillMaxSize()
        .padding(start = start, top = top, end = end),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ThemeDetail(
        state = state,
        dual = false,
        modifier = Modifier
          .fillMaxWidth()
          .height(layout.topHeight)
          .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
      )
      Spacer(Modifier.height(layout.hingeHeight))
      ThemeList(
        state = state,
        onDeleteClick = onDeleteClick,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = bottom + 16.dp),
        // Rows across a whole unfolded screen leave their colors far from their names
        modifier = Modifier
          .weight(1f)
          .widthIn(max = TabletopListMaxWidth)
          .fillMaxWidth(),
      )
    }

    is PreviewLayout.Side -> Row(modifier.fillMaxSize().padding(start = start, top = top, end = end)) {
      ThemeList(
        state = state,
        onDeleteClick = onDeleteClick,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 8.dp, bottom = bottom + 24.dp),
        modifier = Modifier
          .width(layout.controlsWidth)
          .fillMaxHeight(),
      )
      Spacer(Modifier.width(layout.gap))
      ThemeDetail(
        state = state,
        dual = layout.dual,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .padding(top = 8.dp, end = SidePreviewEndPadding, bottom = bottom + 24.dp),
      )
    }
  }
}

/** Light, Dark and System, as connected buttons. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ThemeModeButtons(
  mode: ThemeMode,
  onModeChange: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    ThemeMode.entries.forEachIndexed { index, option ->
      OutlinedToggleButton(
        checked = option == mode,
        onCheckedChange = { onModeChange(option) },
        shapes = when (index) {
          0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
          ThemeMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
          else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
        colors = ToggleButtonDefaults.outlinedToggleButtonColors(
          checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
          checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        // Narrow padding, so "System" fits beside its icon on a 360dp phone
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier
          .weight(1f)
          .semantics { role = Role.RadioButton },
      ) {
        Icon(option.icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(option.label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

private val ThemeMode.icon: ImageVector
  get() = when (this) {
    ThemeMode.LIGHT -> CampfireIcons.Rounded.LightMode
    ThemeMode.DARK -> CampfireIcons.Rounded.DarkMode
    ThemeMode.SYSTEM -> CampfireIcons.Rounded.Brightness6
  }

@Composable
private fun ThemeMode.label(): String = stringResource(
  when (this) {
    ThemeMode.LIGHT -> Res.string.themes_mode_light
    ThemeMode.DARK -> Res.string.themes_mode_dark
    ThemeMode.SYSTEM -> Res.string.themes_mode_system
  },
)

/** The icon the theme shows in the app's search bar, in the surrounding theme's colors. */
@Composable
internal fun themeIcon(theme: AppTheme): Painter = when (theme) {
  is AppTheme.Fixed -> rememberVectorPainter(theme.icon.icon())
  AppTheme.Dynamic -> rememberWallVectorPainter()
}

@Composable
internal fun AppTheme.displayName(): String = when (this) {
  is AppTheme.Fixed.Custom -> name
  is AppTheme.Fixed.Ai -> name
  AppTheme.Fixed.Tent -> stringResource(Res.string.theme_name_tent)
  AppTheme.Fixed.Forest -> stringResource(Res.string.theme_name_forest)
  AppTheme.Fixed.WaterBottle -> stringResource(Res.string.theme_name_water_bottle)
  AppTheme.Fixed.Rucksack -> stringResource(Res.string.theme_name_rucksack)
  AppTheme.Fixed.LifeFloat -> stringResource(Res.string.theme_name_life_float)
  AppTheme.Fixed.Mountain -> stringResource(Res.string.theme_name_mountain)
  AppTheme.Dynamic -> stringResource(Res.string.theme_name_dynamic)
}

/** Whether the user made this theme, rather than it coming with the app. */
internal val AppTheme.isUserTheme: Boolean
  get() = this is AppTheme.Fixed.Custom || this is AppTheme.Fixed.Ai

/** Its main color in light mode, which seeds a new theme customized from it. */
@Composable
internal fun AppTheme.seedColor(): Color = colorScheme(this, useDarkColors = false).primary

internal val ModeButtonsMaxWidth = 480.dp

private val TabletopListMaxWidth = 600.dp
