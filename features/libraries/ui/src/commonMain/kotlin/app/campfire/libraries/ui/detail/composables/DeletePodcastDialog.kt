// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.alpha
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.dialog_delete_podcast_action_cancel
import campfire.features.libraries.ui.generated.resources.dialog_delete_podcast_action_confirm
import campfire.features.libraries.ui.generated.resources.dialog_delete_podcast_action_confirm_hard
import campfire.features.libraries.ui.generated.resources.dialog_delete_podcast_hard_description
import campfire.features.libraries.ui.generated.resources.dialog_delete_podcast_hard_label
import campfire.features.libraries.ui.generated.resources.dialog_delete_podcast_message
import campfire.features.libraries.ui.generated.resources.dialog_delete_podcast_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeletePodcastDialog(
  onConfirm: (hardDelete: Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var hardDelete by remember { mutableStateOf(false) }

  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(Res.string.dialog_delete_podcast_title)) },
    text = {
      Column {
        Text(stringResource(Res.string.dialog_delete_podcast_message))
        Spacer(Modifier.height(16.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .toggleable(
              value = hardDelete,
              role = Role.Switch,
              onValueChange = { hardDelete = it },
            )
            .padding(vertical = 4.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(Res.string.dialog_delete_podcast_hard_label),
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = stringResource(Res.string.dialog_delete_podcast_hard_description),
              style = MaterialTheme.typography.bodySmall.alpha(0.7f),
            )
          }
          Switch(
            checked = hardDelete,
            onCheckedChange = null,
            modifier = Modifier.padding(start = 16.dp),
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(hardDelete) },
        colors = ButtonDefaults.textButtonColors(
          contentColor = MaterialTheme.colorScheme.error,
        ),
      ) {
        Text(
          stringResource(
            if (hardDelete) {
              Res.string.dialog_delete_podcast_action_confirm_hard
            } else {
              Res.string.dialog_delete_podcast_action_confirm
            },
          ),
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(Res.string.dialog_delete_podcast_action_cancel))
      }
    },
  )
}
