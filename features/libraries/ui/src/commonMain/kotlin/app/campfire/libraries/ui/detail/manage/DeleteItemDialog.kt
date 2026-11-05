// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_action_cancel
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_action_hard
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_action_hard_again
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_action_remove
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_hard
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_hard_description
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_lists
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_progress
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_remove
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_remove_description
import campfire.features.libraries.ui.generated.resources.dialog_delete_item_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Asks how to delete an item: from its library only, which a scan undoes, or its files too, which
 * nothing undoes and so takes a second press. Removing from the library is always the default.
 */
@Composable
internal fun DeleteItemDialog(
  itemTitle: String,
  onConfirm: (hardDelete: Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var hardDelete by rememberSaveable { mutableStateOf(false) }
  var pressedOnce by rememberSaveable { mutableStateOf(false) }

  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(Res.string.dialog_delete_item_title, itemTitle)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.selectableGroup()) {
          DeleteOption(
            selected = !hardDelete,
            label = Res.string.dialog_delete_item_remove,
            description = Res.string.dialog_delete_item_remove_description,
            onSelect = {
              hardDelete = false
              pressedOnce = false
            },
            modifier = Modifier.testTag("delete_option_remove"),
          )
          DeleteOption(
            selected = hardDelete,
            label = Res.string.dialog_delete_item_hard,
            description = Res.string.dialog_delete_item_hard_description,
            labelColor = MaterialTheme.colorScheme.error,
            onSelect = { hardDelete = true },
            modifier = Modifier.testTag("delete_option_hard"),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Consequence(stringResource(Res.string.dialog_delete_item_progress))
          Consequence(stringResource(Res.string.dialog_delete_item_lists))
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          when {
            !hardDelete -> onConfirm(false)
            pressedOnce -> onConfirm(true)
            else -> pressedOnce = true
          }
        },
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("delete_confirm"),
      ) {
        Text(
          stringResource(
            when {
              !hardDelete -> Res.string.dialog_delete_item_action_remove
              pressedOnce -> Res.string.dialog_delete_item_action_hard_again
              else -> Res.string.dialog_delete_item_action_hard
            },
          ),
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(Res.string.dialog_delete_item_action_cancel))
      }
    },
  )
}

@Composable
private fun DeleteOption(
  selected: Boolean,
  label: StringResource,
  description: StringResource,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
  labelColor: Color = Color.Unspecified,
) {
  Row(
    verticalAlignment = Alignment.Top,
    modifier = modifier
      .fillMaxWidth()
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
      .padding(vertical = 4.dp),
  ) {
    RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(end = 12.dp, top = 2.dp))
    Column {
      Text(
        text = stringResource(label),
        style = MaterialTheme.typography.titleSmall,
        color = labelColor,
      )
      Text(
        text = stringResource(description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** One of the things deleting does whichever way it's done. */
@Composable
private fun Consequence(
  text: String,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier,
  ) {
    Box(
      Modifier
        .size(5.dp)
        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
