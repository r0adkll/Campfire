// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AutoAwesome
import app.campfire.common.compose.icons.rounded.ContentCopy
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.FormatPaint
import app.campfire.common.compose.icons.rounded.MoreVert
import app.campfire.common.compose.icons.theme.Ai
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.colorScheme
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.themes_copy_name
import campfire.ui.theming.ui.generated.resources.themes_customize_theme
import campfire.ui.theming.ui.generated.resources.themes_delete
import campfire.ui.theming.ui.generated.resources.themes_delete_body
import campfire.ui.theming.ui.generated.resources.themes_delete_cancel
import campfire.ui.theming.ui.generated.resources.themes_delete_current_body
import campfire.ui.theming.ui.generated.resources.themes_delete_title
import campfire.ui.theming.ui.generated.resources.themes_duplicate
import campfire.ui.theming.ui.generated.resources.themes_edit
import campfire.ui.theming.ui.generated.resources.themes_more_options
import org.jetbrains.compose.resources.stringResource

/** The ⋮ button on one of the user's themes, opening its [menu]. */
@Composable
private fun ThemeMenuButton(
  name: String,
  modifier: Modifier = Modifier,
  menu: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
  Box(modifier) {
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

/**
 * The action at the end of a theme's tile, or of a row of the user's own. A built-in theme can only
 * be customized into a new one, so its tile gets that as an extra small button of its own; the list
 * leaves it to the header beside it. The user's themes get the ⋮ menu.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ThemeAction(
  theme: AppTheme,
  eventSink: (ThemePickerUiEvent) -> Unit,
  onDeleteClick: (AppTheme.Fixed) -> Unit,
  modifier: Modifier = Modifier,
) {
  val name = theme.displayName()
  if (theme is AppTheme.Fixed && theme.isUserTheme) {
    ThemeMenuButton(name = name, modifier = modifier) { close ->
      ThemeMenuItems(theme, eventSink, onDeleteClick, close)
    }
  } else {
    val seedColor = theme.seedColor()
    val label = stringResource(Res.string.themes_customize_theme, name)
    IconButtonTooltip(text = label, modifier = modifier) {
      // Extra small, so it reads as secondary to choosing the theme.
      IconButton(
        onClick = { eventSink(ThemePickerUiEvent.Customize(theme, seedColor)) },
        shapes = IconButtonDefaults.shapes(),
        modifier = Modifier.size(IconButtonDefaults.extraSmallContainerSize()),
      ) {
        Icon(
          CampfireIcons.Rounded.FormatPaint,
          contentDescription = label,
          modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
        )
      }
    }
  }
}

/** One of the user's themes can be edited, duplicated or deleted ([onDeleteClick] asks first). */
@Composable
private fun ThemeMenuItems(
  theme: AppTheme.Fixed,
  eventSink: (ThemePickerUiEvent) -> Unit,
  onDeleteClick: (AppTheme.Fixed) -> Unit,
  close: () -> Unit,
) {
  ThemeMenuItem(
    text = stringResource(Res.string.themes_edit),
    icon = if (theme is AppTheme.Fixed.Ai) CampfireIcons.Rounded.AutoAwesome else CampfireIcons.Rounded.Edit,
    onClick = {
      close()
      eventSink(ThemePickerUiEvent.OpenThemeBuilder(theme))
    },
  )
  val copyName = stringResource(Res.string.themes_copy_name, theme.displayName())
  ThemeMenuItem(
    text = stringResource(Res.string.themes_duplicate),
    icon = CampfireIcons.Rounded.ContentCopy,
    onClick = {
      close()
      eventSink(ThemePickerUiEvent.Duplicate(theme, copyName))
    },
  )
  ThemeMenuItem(
    text = stringResource(Res.string.themes_delete),
    icon = CampfireIcons.Rounded.Delete,
    destructive = true,
    onClick = {
      close()
      onDeleteClick(theme)
    },
  )
}

@Composable
private fun ThemeMenuItem(
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
internal fun DeleteThemeDialog(
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
