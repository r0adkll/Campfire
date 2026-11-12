// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.dialog

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
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.dialog_delete_action_cancel
import campfire.common.compose.generated.resources.dialog_delete_episode_action_hard
import campfire.common.compose.generated.resources.dialog_delete_episode_action_hard_again
import campfire.common.compose.generated.resources.dialog_delete_episode_action_remove
import campfire.common.compose.generated.resources.dialog_delete_episode_hard
import campfire.common.compose.generated.resources.dialog_delete_episode_hard_description
import campfire.common.compose.generated.resources.dialog_delete_episode_lists
import campfire.common.compose.generated.resources.dialog_delete_episode_progress
import campfire.common.compose.generated.resources.dialog_delete_episode_remove
import campfire.common.compose.generated.resources.dialog_delete_episode_remove_description
import campfire.common.compose.generated.resources.dialog_delete_episode_title
import org.jetbrains.compose.resources.stringResource

/**
 * Asks how to delete a podcast episode from the server: from its podcast only, which a library
 * scan undoes, or its audio file too, which nothing undoes and so takes a second press.
 */
@Composable
fun DeleteEpisodeDialog(
  episodeTitle: String,
  onConfirm: (hardDelete: Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DeleteFromServerDialog(
    title = stringResource(Res.string.dialog_delete_episode_title, episodeTitle.asDeleteQuestionSubject()),
    removeLabel = stringResource(Res.string.dialog_delete_episode_remove),
    removeDescription = stringResource(Res.string.dialog_delete_episode_remove_description),
    hardDeleteLabel = stringResource(Res.string.dialog_delete_episode_hard),
    hardDeleteDescription = stringResource(Res.string.dialog_delete_episode_hard_description),
    consequences = listOf(
      stringResource(Res.string.dialog_delete_episode_progress),
      stringResource(Res.string.dialog_delete_episode_lists),
    ),
    removeAction = stringResource(Res.string.dialog_delete_episode_action_remove),
    hardDeleteAction = stringResource(Res.string.dialog_delete_episode_action_hard),
    hardDeleteAgainAction = stringResource(Res.string.dialog_delete_episode_action_hard_again),
    onConfirm = onConfirm,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  )
}

/**
 * [this] title as the subject of a "Delete …?" question, without a question mark of its own, so a
 * title that asks something ("Is It Real?") doesn't end the question twice.
 */
fun String.asDeleteQuestionSubject(): String = trimEnd().removeSuffix("?")

/**
 * Asks how to delete something from the server: only taking it out of the library, which a scan
 * undoes, or deleting its files from disk too, which nothing undoes and so takes a second press.
 * Taking it out is always the default.
 *
 * @param consequences what deleting does whichever way it's done, listed under the choice.
 * @param removeAction the confirm button's label while taking it out is chosen.
 * @param hardDeleteAction the confirm button's label while deleting the files is chosen.
 * @param hardDeleteAgainAction the confirm button's label once pressed with deleting the files
 *   chosen, asking to press again.
 * @param onConfirm called with whether to delete the files too.
 */
@Composable
fun DeleteFromServerDialog(
  title: String,
  removeLabel: String,
  removeDescription: String,
  hardDeleteLabel: String,
  hardDeleteDescription: String,
  consequences: List<String>,
  removeAction: String,
  hardDeleteAction: String,
  hardDeleteAgainAction: String,
  onConfirm: (hardDelete: Boolean) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var hardDelete by rememberSaveable { mutableStateOf(false) }
  var pressedOnce by rememberSaveable { mutableStateOf(false) }

  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.selectableGroup()) {
          DeleteOption(
            selected = !hardDelete,
            label = removeLabel,
            description = removeDescription,
            onSelect = {
              hardDelete = false
              pressedOnce = false
            },
            modifier = Modifier.testTag("delete_option_remove"),
          )
          DeleteOption(
            selected = hardDelete,
            label = hardDeleteLabel,
            description = hardDeleteDescription,
            labelColor = MaterialTheme.colorScheme.error,
            onSelect = { hardDelete = true },
            modifier = Modifier.testTag("delete_option_hard"),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          consequences.forEach { Consequence(it) }
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
          when {
            !hardDelete -> removeAction
            pressedOnce -> hardDeleteAgainAction
            else -> hardDeleteAction
          },
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(Res.string.dialog_delete_action_cancel))
      }
    },
  )
}

@Composable
private fun DeleteOption(
  selected: Boolean,
  label: String,
  description: String,
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
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = labelColor,
      )
      Text(
        text = description,
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
