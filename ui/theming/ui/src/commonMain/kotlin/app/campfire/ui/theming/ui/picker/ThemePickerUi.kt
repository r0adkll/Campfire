// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AutoAwesome
import app.campfire.common.compose.icons.rounded.Brightness6
import app.campfire.common.compose.icons.rounded.Check
import app.campfire.common.compose.icons.rounded.ContentCopy
import app.campfire.common.compose.icons.rounded.DarkMode
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.FormatPaint
import app.campfire.common.compose.icons.rounded.LightMode
import app.campfire.common.compose.icons.rounded.MoreVert
import app.campfire.common.compose.icons.theme.Ai
import app.campfire.common.compose.icons.theme.Palette
import app.campfire.common.compose.icons.theme.rememberWallVectorPainter
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeImage
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.PreviewScene
import app.campfire.ui.theming.ui.preview.ThemePreview
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_name_dynamic
import campfire.ui.theming.ui.generated.resources.theme_name_forest
import campfire.ui.theming.ui.generated.resources.theme_name_life_float
import campfire.ui.theming.ui.generated.resources.theme_name_mountain
import campfire.ui.theming.ui.generated.resources.theme_name_rucksack
import campfire.ui.theming.ui.generated.resources.theme_name_tent
import campfire.ui.theming.ui.generated.resources.theme_name_water_bottle
import campfire.ui.theming.ui.generated.resources.themes_built_in
import campfire.ui.theming.ui.generated.resources.themes_copy_name
import campfire.ui.theming.ui.generated.resources.themes_create_ai
import campfire.ui.theming.ui.generated.resources.themes_customize
import campfire.ui.theming.ui.generated.resources.themes_delete
import campfire.ui.theming.ui.generated.resources.themes_delete_body
import campfire.ui.theming.ui.generated.resources.themes_delete_cancel
import campfire.ui.theming.ui.generated.resources.themes_delete_current_body
import campfire.ui.theming.ui.generated.resources.themes_delete_title
import campfire.ui.theming.ui.generated.resources.themes_duplicate
import campfire.ui.theming.ui.generated.resources.themes_edit
import campfire.ui.theming.ui.generated.resources.themes_load_error
import campfire.ui.theming.ui.generated.resources.themes_mode_dark
import campfire.ui.theming.ui.generated.resources.themes_mode_light
import campfire.ui.theming.ui.generated.resources.themes_mode_system
import campfire.ui.theming.ui.generated.resources.themes_more_options
import campfire.ui.theming.ui.generated.resources.themes_new
import campfire.ui.theming.ui.generated.resources.themes_title
import campfire.ui.theming.ui.generated.resources.themes_yours
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Every theme as a small copy of Home in its colors, built-in themes first and then the user's
 * own, with the light and dark setting above them. Picking a theme applies it straight away, so
 * the screen itself recolors.
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
      ThemeGallery(state = state, contentPadding = paddingValues)
    }
  }
}

@Composable
private fun ThemeGallery(
  state: ThemePickerUiState,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  val layoutDirection = LocalLayoutDirection.current

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

  LazyVerticalGrid(
    columns = GridCells.Adaptive(TileMinWidth),
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = contentPadding.calculateStartPadding(layoutDirection) + GalleryPadding,
      top = contentPadding.calculateTopPadding() + 8.dp,
      end = contentPadding.calculateEndPadding(layoutDirection) + GalleryPadding,
      bottom = contentPadding.calculateBottomPadding() + 24.dp,
    ),
    horizontalArrangement = Arrangement.spacedBy(TileSpacing),
    verticalArrangement = Arrangement.spacedBy(TileSpacing),
  ) {
    item(key = "mode", span = { GridItemSpan(maxLineSpan) }, contentType = "mode") {
      ThemeModeButtons(
        mode = state.themeMode,
        onModeChange = { state.eventSink(ThemePickerUiEvent.SetThemeMode(it)) },
        modifier = Modifier
          .wrapContentWidth(Alignment.Start)
          .widthIn(max = ModeButtonsMaxWidth),
      )
    }

    header(key = "built-in", text = Res.string.themes_built_in)
    items(state.builtInThemes, key = { "built-in:" + it.key }, contentType = { "theme" }) { theme ->
      ThemeTile(
        theme = theme,
        selected = theme.key == state.currentTheme.key,
        previewContent = state.previewContent,
        onClick = { state.eventSink(ThemePickerUiEvent.SelectTheme(theme)) },
        menu = { close ->
          // Its main color in light mode seeds the new theme, as a theme's seed sets its accent.
          val seedColor = colorScheme(theme, useDarkColors = false).primary
          TileMenuItem(
            text = stringResource(Res.string.themes_customize),
            icon = CampfireIcons.Rounded.FormatPaint,
            onClick = {
              close()
              state.eventSink(ThemePickerUiEvent.Customize(theme, seedColor))
            },
          )
        },
      )
    }

    header(key = "yours", text = Res.string.themes_yours)
    when (val customThemes = state.customThemes) {
      LoadState.Loading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
          CircularWavyProgressIndicator(Modifier.size(40.dp))
        }
      }
      LoadState.Error -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
        Text(
          text = stringResource(Res.string.themes_load_error),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
      }
      is LoadState.Loaded -> items(customThemes.data, key = { "custom:" + it.id }, contentType = { "theme" }) { theme ->
        ThemeTile(
          theme = theme,
          selected = theme.key == state.currentTheme.key,
          previewContent = state.previewContent,
          onClick = { state.eventSink(ThemePickerUiEvent.SelectTheme(theme)) },
          menu = { close ->
            TileMenuItem(
              text = stringResource(Res.string.themes_edit),
              icon = if (theme is AppTheme.Fixed.Ai) CampfireIcons.Rounded.AutoAwesome else CampfireIcons.Rounded.Edit,
              onClick = {
                close()
                state.eventSink(ThemePickerUiEvent.OpenThemeBuilder(theme))
              },
            )
            val copyName = stringResource(Res.string.themes_copy_name, theme.displayName())
            TileMenuItem(
              text = stringResource(Res.string.themes_duplicate),
              icon = CampfireIcons.Rounded.ContentCopy,
              onClick = {
                close()
                state.eventSink(ThemePickerUiEvent.Duplicate(theme, copyName))
              },
            )
            TileMenuItem(
              text = stringResource(Res.string.themes_delete),
              icon = CampfireIcons.Rounded.Delete,
              destructive = true,
              onClick = {
                close()
                pendingDeleteId = theme.id
              },
            )
          },
        )
      }
    }

    item(key = "new", contentType = "create") {
      CreateThemeTile(
        icon = CampfireIcons.Theme.Palette,
        label = stringResource(Res.string.themes_new),
        onClick = { state.eventSink(ThemePickerUiEvent.OpenThemeBuilder()) },
      )
    }
    if (state.aiThemeBuilderAvailable) {
      item(key = "ai", contentType = "create") {
        CreateThemeTile(
          icon = CampfireIcons.Theme.Ai,
          label = stringResource(Res.string.themes_create_ai),
          onClick = { state.eventSink(ThemePickerUiEvent.OpenAiThemeBuilder) },
        )
      }
    }
  }
}

private fun LazyGridScope.header(key: String, text: StringResource) {
  item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
    Text(
      text = stringResource(text),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .padding(start = 4.dp, top = 8.dp)
        .semantics { heading() },
    )
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

/**
 * A theme as Home drawn in its colors, over its icon and name. The whole tile picks the theme;
 * the preview's own cards don't take taps.
 */
@Composable
internal fun ThemeTile(
  theme: AppTheme,
  selected: Boolean,
  previewContent: PreviewContent,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val ringColor by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
  )
  val name = theme.displayName()
  Column(
    modifier = modifier
      .selectable(
        selected = selected,
        interactionSource = interactionSource,
        indication = null,
        role = Role.RadioButton,
        onClick = onClick,
      )
      // The tile carries its name itself, so screen readers announce a named choice
      .semantics { contentDescription = name },
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .selectionRing { ringColor }
        .padding(RingInset),
    ) {
      // A thumbnail keeps the default text size: larger text only crowds and clips at this size.
      val density = LocalDensity.current
      CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        MaterialTheme(colorScheme = colorScheme(theme)) {
          ThemePreview(
            scene = PreviewScene.Home,
            content = previewContent,
            icon = themeIcon(theme),
            shape = RoundedCornerShape(TileCorner),
            coverColorsNote = false,
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(TileAspectRatio)
              .clearAndSetSemantics {},
          )
        }
      }
      // On top of the preview, so a tap on one of its cards picks the theme instead.
      Box(
        Modifier
          .matchParentSize()
          .clearAndSetSemantics {},
      ) {
        Box(
          Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(TileCorner))
            .indication(interactionSource, ripple())
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        )
      }
      if (selected) {
        // Inside the selected preview's top corner
        SelectedBadge(
          Modifier
            .align(Alignment.TopEnd)
            .padding(BadgeInset),
        )
      }
    }

    Row(
      modifier = Modifier
        .heightIn(min = 40.dp)
        .padding(start = RingInset),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AppThemeImage(appTheme = theme, modifier = Modifier.size(24.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .weight(1f)
          .clearAndSetSemantics {},
      )
      if (menu != null) {
        Box {
          var expanded by remember { mutableStateOf(false) }
          val menuLabel = stringResource(Res.string.themes_more_options, name)
          IconButtonTooltip(text = menuLabel) {
            IconButton(onClick = { expanded = true }) {
              Icon(CampfireIcons.Rounded.MoreVert, contentDescription = menuLabel)
            }
          }
          DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menu { expanded = false }
          }
        }
      }
    }
  }
}

@Composable
private fun TileMenuItem(
  text: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  destructive: Boolean = false,
) {
  val colors = if (destructive) {
    MenuDefaults.itemColors(
      textColor = MaterialTheme.colorScheme.error,
      leadingIconColor = MaterialTheme.colorScheme.error,
    )
  } else {
    MenuDefaults.itemColors()
  }
  DropdownMenuItem(
    text = { Text(text) },
    leadingIcon = { Icon(icon, contentDescription = null) },
    onClick = onClick,
    colors = colors,
    modifier = modifier,
  )
}

/** Asks before deleting one of the user's themes, saying when it's the theme in use. */
@Composable
private fun DeleteThemeDialog(
  theme: AppTheme.Fixed,
  isCurrent: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.themes_delete_title, theme.displayName())) },
    text = {
      Text(
        stringResource(if (isCurrent) Res.string.themes_delete_current_body else Res.string.themes_delete_body),
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(Res.string.themes_delete), color = MaterialTheme.colorScheme.error)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.themes_delete_cancel)) }
    },
    modifier = modifier,
  )
}

@Composable
private fun SelectedBadge(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .size(24.dp)
      .background(MaterialTheme.colorScheme.primary, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      CampfireIcons.Rounded.Check,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier.size(16.dp),
    )
  }
}

/** A tile the size of a theme's preview, outlined in dashes, that starts a new theme. */
@Composable
private fun CreateThemeTile(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val outline = MaterialTheme.colorScheme.outline
  Box(modifier.padding(RingInset)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(TileAspectRatio)
        .clip(RoundedCornerShape(TileCorner))
        .dashedOutline(outline, TileCorner)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(12.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(icon, contentDescription = null, modifier = Modifier.size(48.dp))
      Spacer(Modifier.height(12.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
    }
  }
}

/** The ring around the selected theme, drawn behind the tile so its check sits on top. */
private fun Modifier.selectionRing(color: () -> Color): Modifier = drawBehind {
  val width = SelectionRingWidth.toPx()
  drawRoundRect(
    color = color(),
    topLeft = Offset(width / 2, width / 2),
    size = Size(size.width - width, size.height - width),
    cornerRadius = CornerRadius((TileCorner + RingInset).toPx() - width / 2),
    style = Stroke(width = width),
  )
}

private fun Modifier.dashedOutline(color: Color, corner: Dp): Modifier = drawBehind {
  val width = 2.dp.toPx()
  val dash = 8.dp.toPx()
  drawRoundRect(
    color = color,
    topLeft = Offset(width / 2, width / 2),
    size = Size(size.width - width, size.height - width),
    cornerRadius = CornerRadius(corner.toPx() - width / 2),
    style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.75f))),
  )
}

/** The icon the theme shows in the app's search bar, in the surrounding theme's colors. */
@Composable
private fun themeIcon(theme: AppTheme): Painter = when (theme) {
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

/** Two tiles across any phone, from 360dp wide. */
private val TileMinWidth = 148.dp
private val TileSpacing = 12.dp
private val GalleryPadding = 16.dp
private val ModeButtonsMaxWidth = 480.dp

/** The Home preview's own proportions, so it draws whole without empty space. */
private const val TileAspectRatio = 360f / 380f
private val TileCorner = 16.dp
private val SelectionRingWidth = 3.dp

/** The ring sits a little off the preview, with this much from its outside to the preview. */
private val RingInset = 6.dp

/** How far the selected check sits in from the preview's corner. */
private val BadgeInset = 8.dp
