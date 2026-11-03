// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.theme.Ai
import app.campfire.common.compose.icons.theme.Palette
import app.campfire.core.coroutines.LoadState
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeImage
import app.campfire.ui.theming.api.colorScheme
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.themes_built_in
import campfire.ui.theming.ui.generated.resources.themes_create_ai
import campfire.ui.theming.ui.generated.resources.themes_new
import campfire.ui.theming.ui.generated.resources.themes_yours
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Foldables and tablets: the themes as a list of rows beside [ThemeDetail]'s large preview,
 * under the light and dark setting. Built-in themes come first, then the user's own.
 */
@Composable
internal fun ThemeList(
  state: ThemePickerUiState,
  onDeleteClick: (AppTheme.Fixed) -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item(key = "mode", contentType = "mode") {
      ThemeModeButtons(
        mode = state.themeMode,
        onModeChange = { state.eventSink(ThemePickerUiEvent.SetThemeMode(it)) },
        modifier = Modifier.padding(bottom = 4.dp),
      )
    }

    header(key = "built-in", text = Res.string.themes_built_in)
    items(state.builtInThemes, key = { "built-in:" + it.key }, contentType = { "theme" }) { theme ->
      ThemeRow(
        theme = theme,
        selected = theme.key == state.currentTheme.key,
        onClick = { state.eventSink(ThemePickerUiEvent.SelectTheme(theme)) },
        menu = { close -> ThemeMenuItems(theme, state.eventSink, onDeleteClick, close) },
      )
    }

    header(key = "yours", text = Res.string.themes_yours)
    when (val customThemes = state.customThemes) {
      LoadState.Loading -> item(key = "loading") { CustomThemesLoading() }
      LoadState.Error -> item(key = "error") { CustomThemesError() }
      is LoadState.Loaded -> items(customThemes.data, key = { "custom:" + it.id }, contentType = { "theme" }) { theme ->
        ThemeRow(
          theme = theme,
          selected = theme.key == state.currentTheme.key,
          onClick = { state.eventSink(ThemePickerUiEvent.SelectTheme(theme)) },
          menu = { close -> ThemeMenuItems(theme, state.eventSink, onDeleteClick, close) },
        )
      }
    }

    item(key = "new", contentType = "create") {
      CreateThemeRow(
        icon = CampfireIcons.Theme.Palette,
        label = stringResource(Res.string.themes_new),
        onClick = { state.eventSink(ThemePickerUiEvent.OpenThemeBuilder()) },
      )
    }
    if (state.aiThemeBuilderAvailable) {
      item(key = "ai", contentType = "create") {
        CreateThemeRow(
          icon = CampfireIcons.Theme.Ai,
          label = stringResource(Res.string.themes_create_ai),
          onClick = { state.eventSink(ThemePickerUiEvent.OpenAiThemeBuilder) },
        )
      }
    }
  }
}

private fun LazyListScope.header(key: String, text: StringResource) {
  item(key = key, contentType = "header") {
    SectionHeader(stringResource(text))
  }
}

/**
 * A theme as a calm row: a radio button, its icon and name, and a strip of its colors. The
 * large preview beside the list shows how it looks.
 */
@Composable
internal fun ThemeRow(
  theme: AppTheme,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
  val name = theme.displayName()
  val colors = MaterialTheme.colorScheme
  val container by animateColorAsState(
    targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
  )
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = RowHeight)
      .clip(RowShape)
      .background(container)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
      .semantics { contentDescription = name }
      .padding(start = 4.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 8.dp))
    AppThemeImage(appTheme = theme, modifier = Modifier.size(28.dp))
    Spacer(Modifier.width(12.dp))
    Text(
      text = name,
      style = MaterialTheme.typography.titleSmall,
      color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .weight(1f)
        .padding(vertical = 8.dp)
        .clearAndSetSemantics {},
    )
    ColorStrip(theme, Modifier.padding(start = 8.dp))
    if (menu != null) {
      ThemeMenuButton(name = name, menu = menu)
    } else {
      Spacer(Modifier.width(12.dp))
    }
  }
}

/** Four of the theme's colors, in the current light or dark mode: a glance at its palette. */
@Composable
private fun ColorStrip(theme: AppTheme, modifier: Modifier = Modifier) {
  val scheme = colorScheme(theme)
  val outline = MaterialTheme.colorScheme.outlineVariant
  Row(
    modifier = modifier
      .size(width = 56.dp, height = 20.dp)
      .clip(CircleShape)
      .border(1.dp, outline, CircleShape),
  ) {
    listOf(scheme.primary, scheme.secondaryContainer, scheme.tertiaryContainer, scheme.surfaceContainerHighest)
      .forEach { color ->
        Box(
          Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(color),
        )
      }
  }
}

/** A row, outlined in dashes, that starts a new theme. */
@Composable
private fun CreateThemeRow(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = RowHeight)
      .clip(RowShape)
      .dashedOutline(MaterialTheme.colorScheme.outline, RowCorner)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(icon, contentDescription = null, modifier = Modifier.size(28.dp))
    Spacer(Modifier.width(12.dp))
    Text(text = label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
  }
}

private val RowHeight = 64.dp
private val RowCorner = 16.dp
private val RowShape = RoundedCornerShape(RowCorner)
