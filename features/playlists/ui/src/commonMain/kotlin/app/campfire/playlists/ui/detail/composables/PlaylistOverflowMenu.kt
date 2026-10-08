// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.MarkFinished
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.icons.rounded.MoreVert
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.action_more_actions
import campfire.features.playlists.ui.generated.resources.menu_item_mark_all_finished
import campfire.features.playlists.ui.generated.resources.menu_item_mark_all_not_finished
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlaylistOverflowMenu(
  canMarkAllFinished: Boolean,
  canMarkAllNotFinished: Boolean,
  onMarkAllFinished: () -> Unit,
  onMarkAllNotFinished: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier) {
    val moreActionsLabel = stringResource(Res.string.action_more_actions)
    IconButtonTooltip(text = moreActionsLabel) {
      IconButton(onClick = { expanded = true }) {
        Icon(
          CampfireIcons.Rounded.MoreVert,
          contentDescription = moreActionsLabel,
        )
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      shape = MaterialTheme.shapes.medium,
    ) {
      if (canMarkAllFinished) {
        DropdownMenuItem(
          text = { Text(stringResource(Res.string.menu_item_mark_all_finished)) },
          leadingIcon = {
            Icon(CampfireIcons.Rounded.MarkFinished, contentDescription = null)
          },
          onClick = {
            expanded = false
            onMarkAllFinished()
          },
        )
      }
      if (canMarkAllNotFinished) {
        DropdownMenuItem(
          text = { Text(stringResource(Res.string.menu_item_mark_all_not_finished)) },
          leadingIcon = {
            Icon(CampfireIcons.Filled.MarkFinished, contentDescription = null)
          },
          onClick = {
            expanded = false
            onMarkAllNotFinished()
          },
        )
      }
    }
  }
}
