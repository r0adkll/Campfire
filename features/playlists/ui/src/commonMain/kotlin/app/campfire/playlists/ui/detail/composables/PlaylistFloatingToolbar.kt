// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail.composables

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.PlaylistPlay
import app.campfire.common.compose.icons.rounded.SwapCalls
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.action_delete_playlist
import campfire.features.playlists.ui.generated.resources.action_download_playlist
import campfire.features.playlists.ui.generated.resources.action_edit_playlist
import campfire.features.playlists.ui.generated.resources.action_play_all
import campfire.features.playlists.ui.generated.resources.action_reorder_playlist
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaylistFloatingToolbar(
  isReordering: Boolean,
  onPlayAllClick: () -> Unit,
  onEditClick: () -> Unit,
  onReorderChange: (Boolean) -> Unit,
  onDownloadClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
  expanded: Boolean = true,
) {
  HorizontalFloatingToolbar(
    expanded = true,
    floatingActionButton = {
      val playAllLabel = stringResource(Res.string.action_play_all)
      IconButtonTooltip(text = playAllLabel) {
        FloatingToolbarDefaults.StandardFloatingActionButton(
          onClick = onPlayAllClick,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
          Icon(CampfireIcons.Rounded.PlaylistPlay, contentDescription = playAllLabel)
        }
      }
    },
    floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.Start,
    modifier = modifier,
  ) {
    ToolbarButton(
      CampfireIcons.Rounded.Download,
      onClick = onDownloadClick,
      expanded = expanded,
      contentDescription = stringResource(Res.string.action_download_playlist),
    )
    ToolbarButton(
      CampfireIcons.Rounded.Edit,
      onClick = onEditClick,
      expanded = expanded,
      contentDescription = stringResource(Res.string.action_edit_playlist),
    )
    ToggleToolbarButton(
      checked = isReordering,
      onCheckedChange = onReorderChange,
      icon = CampfireIcons.Rounded.SwapCalls,
      expanded = expanded,
      contentDescription = stringResource(Res.string.action_reorder_playlist),
    )
    ToolbarButton(
      CampfireIcons.Rounded.Delete,
      onClick = onDeleteClick,
      expanded = expanded,
      contentDescription = stringResource(Res.string.action_delete_playlist),
      tint = MaterialTheme.colorScheme.error,
    )
  }
}

@Composable
internal fun ToolbarButton(
  icon: ImageVector,
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  expanded: Boolean = false,
  tint: Color = LocalContentColor.current,
) {
  IconButtonTooltip(
    text = contentDescription,
    modifier = modifier,
  ) {
    IconButton(
      onClick = onClick,
      Modifier.focusProperties { canFocus = expanded },
    ) {
      Icon(
        icon,
        contentDescription = contentDescription,
        tint = tint,
      )
    }
  }
}

@Composable
internal fun ToggleToolbarButton(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  expanded: Boolean = false,
) {
  IconButtonTooltip(
    text = contentDescription,
    modifier = modifier,
  ) {
    IconToggleButton(
      checked = checked,
      onCheckedChange = onCheckedChange,
      Modifier.focusProperties { canFocus = expanded },
    ) {
      Icon(icon, contentDescription = contentDescription)
    }
  }
}
