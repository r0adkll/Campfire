// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.icons.rounded.MoreVert
import app.campfire.common.compose.icons.rounded.Replay
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.action_more_actions
import campfire.features.playlists.ui.generated.resources.dialog_action_cancel
import campfire.features.playlists.ui.generated.resources.dialog_action_mark_finished
import campfire.features.playlists.ui.generated.resources.dialog_action_mark_not_finished
import campfire.features.playlists.ui.generated.resources.dialog_mark_all_finished_message
import campfire.features.playlists.ui.generated.resources.dialog_mark_all_finished_title
import campfire.features.playlists.ui.generated.resources.dialog_mark_all_not_finished_message
import campfire.features.playlists.ui.generated.resources.dialog_mark_all_not_finished_title
import campfire.features.playlists.ui.generated.resources.menu_item_mark_all_finished
import campfire.features.playlists.ui.generated.resources.menu_item_mark_all_not_finished
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal enum class PlaylistProgressAction {
  MarkFinished,
  MarkNotFinished,
}

/**
 * Top bar overflow offering the whole-playlist progress actions that would change at least one
 * item: [PlaylistProgressAction.MarkFinished] while any is unfinished, and
 * [PlaylistProgressAction.MarkNotFinished] while any is finished.
 */
@Composable
internal fun PlaylistProgressMenu(
  unfinishedCount: Int,
  finishedCount: Int,
  enabled: Boolean,
  onActionClick: (PlaylistProgressAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier) {
    val moreActionsLabel = stringResource(Res.string.action_more_actions)
    IconButtonTooltip(text = moreActionsLabel) {
      IconButton(
        onClick = { expanded = true },
        enabled = enabled,
      ) {
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
      if (unfinishedCount > 0) {
        DropdownMenuItem(
          text = { Text(stringResource(Res.string.menu_item_mark_all_finished)) },
          leadingIcon = { Icon(CampfireIcons.Rounded.MarkFinished, contentDescription = null) },
          onClick = {
            expanded = false
            onActionClick(PlaylistProgressAction.MarkFinished)
          },
        )
      }
      if (finishedCount > 0) {
        DropdownMenuItem(
          text = { Text(stringResource(Res.string.menu_item_mark_all_not_finished)) },
          leadingIcon = { Icon(CampfireIcons.Rounded.Replay, contentDescription = null) },
          onClick = {
            expanded = false
            onActionClick(PlaylistProgressAction.MarkNotFinished)
          },
        )
      }
    }
  }
}

/** Confirms a whole-playlist progress change, naming how many items it touches. */
@Composable
internal fun ConfirmPlaylistProgressDialog(
  action: PlaylistProgressAction,
  itemCount: Int,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val title = when (action) {
    PlaylistProgressAction.MarkFinished -> Res.string.dialog_mark_all_finished_title
    PlaylistProgressAction.MarkNotFinished -> Res.string.dialog_mark_all_not_finished_title
  }
  val message = when (action) {
    PlaylistProgressAction.MarkFinished -> Res.plurals.dialog_mark_all_finished_message
    PlaylistProgressAction.MarkNotFinished -> Res.plurals.dialog_mark_all_not_finished_message
  }
  val confirmLabel = when (action) {
    PlaylistProgressAction.MarkFinished -> Res.string.dialog_action_mark_finished
    PlaylistProgressAction.MarkNotFinished -> Res.string.dialog_action_mark_not_finished
  }

  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(title)) },
    text = { Text(pluralStringResource(message, itemCount, itemCount)) },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(confirmLabel))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.dialog_action_cancel))
      }
    },
  )
}
