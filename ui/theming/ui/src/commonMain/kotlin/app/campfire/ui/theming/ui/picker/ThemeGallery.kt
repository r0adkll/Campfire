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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Check
import app.campfire.common.compose.icons.theme.Ai
import app.campfire.common.compose.icons.theme.Palette
import app.campfire.core.coroutines.LoadState
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeImage
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.ui.preview.PreviewContent
import app.campfire.ui.theming.ui.preview.PreviewScene
import app.campfire.ui.theming.ui.preview.ThemePreview
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.themes_built_in
import campfire.ui.theming.ui.generated.resources.themes_create_ai
import campfire.ui.theming.ui.generated.resources.themes_load_error
import campfire.ui.theming.ui.generated.resources.themes_new
import campfire.ui.theming.ui.generated.resources.themes_yours
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Phones: every theme as a small copy of Home in its colors, built-in themes first and then the
 * user's own, under the light and dark setting.
 */
@Composable
internal fun ThemeGallery(
  state: ThemePickerUiState,
  onDeleteClick: (AppTheme.Fixed) -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  val layoutDirection = LocalLayoutDirection.current
  LazyVerticalGrid(
    columns = GridCells.Adaptive(TileMinWidth),
    modifier = modifier,
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
        action = { ThemeAction(theme, state.eventSink, onDeleteClick) },
      )
    }

    header(key = "yours", text = Res.string.themes_yours)
    when (val customThemes = state.customThemes) {
      LoadState.Loading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
        CustomThemesLoading()
      }
      LoadState.Error -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
        CustomThemesError()
      }
      is LoadState.Loaded -> items(customThemes.data, key = { "custom:" + it.id }, contentType = { "theme" }) { theme ->
        ThemeTile(
          theme = theme,
          selected = theme.key == state.currentTheme.key,
          previewContent = state.previewContent,
          onClick = { state.eventSink(ThemePickerUiEvent.SelectTheme(theme)) },
          action = { ThemeAction(theme, state.eventSink, onDeleteClick) },
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
    SectionHeader(stringResource(text))
  }
}

/** A section's name, Built-in or Your themes. */
@Composable
internal fun SectionHeader(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = modifier
      .padding(start = 4.dp, top = 8.dp)
      .semantics { heading() },
  )
}

@Composable
internal fun CustomThemesLoading(modifier: Modifier = Modifier) {
  Box(modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
    CircularWavyProgressIndicator(Modifier.size(40.dp))
  }
}

@Composable
internal fun CustomThemesError(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(Res.string.themes_load_error),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.error,
    modifier = modifier.padding(horizontal = 4.dp),
  )
}

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
  action: (@Composable () -> Unit)? = null,
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
      action?.invoke()
    }
  }
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

internal fun Modifier.dashedOutline(color: Color, corner: Dp): Modifier = drawBehind {
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

/** Two tiles across any phone, from 360dp wide. */
private val TileMinWidth = 148.dp
private val TileSpacing = 12.dp
private val GalleryPadding = 16.dp

/** The Home preview's own proportions, so it draws whole without empty space. */
private const val TileAspectRatio = 360f / 380f
private val TileCorner = 16.dp
private val SelectionRingWidth = 3.dp

/** The ring sits a little off the preview, with this much from its outside to the preview. */
private val RingInset = 6.dp

/** How far the selected check sits in from the preview's corner. */
private val BadgeInset = 8.dp
