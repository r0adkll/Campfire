// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.dialog_quick_match_cancel
import campfire.features.libraries.ui.generated.resources.dialog_quick_match_confirm
import campfire.features.libraries.ui.generated.resources.dialog_quick_match_cover_no_undo
import campfire.features.libraries.ui.generated.resources.dialog_quick_match_message
import campfire.features.libraries.ui.generated.resources.dialog_quick_match_no_review
import campfire.features.libraries.ui.generated.resources.dialog_quick_match_replace_cover
import campfire.features.libraries.ui.generated.resources.dialog_quick_match_replace_details
import campfire.features.libraries.ui.generated.resources.dialog_quick_match_title
import org.jetbrains.compose.resources.stringResource

/** What a quick match may replace, beyond filling in what a book is missing. */
data class QuickMatchChoice(
  val replaceDetails: Boolean = false,
  val replaceCover: Boolean = false,
)

/**
 * Asks before quick matching the book titled [itemTitle] against [sourceName]'s first result,
 * which nothing reviews first. It only fills in what the book is missing unless told to replace
 * its details or cover, whatever the server prefers.
 */
@Composable
internal fun QuickMatchDialog(
  itemTitle: String,
  sourceName: String,
  onConfirm: (QuickMatchChoice) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var replaceDetails by rememberSaveable { mutableStateOf(false) }
  var replaceCover by rememberSaveable { mutableStateOf(false) }

  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(Res.string.dialog_quick_match_title)) },
    text = {
      // A phone on its side hasn't the height for all of it
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
      ) {
        Text(stringResource(Res.string.dialog_quick_match_message, itemTitle, sourceName))

        Column {
          ReplaceSwitch(
            checked = replaceDetails,
            label = stringResource(Res.string.dialog_quick_match_replace_details),
            onCheckedChange = { replaceDetails = it },
            modifier = Modifier.testTag("quick_match_replace_details"),
          )
          ReplaceSwitch(
            checked = replaceCover,
            label = stringResource(Res.string.dialog_quick_match_replace_cover),
            description = stringResource(Res.string.dialog_quick_match_cover_no_undo).takeIf { replaceCover },
            onCheckedChange = { replaceCover = it },
            modifier = Modifier.testTag("quick_match_replace_cover"),
          )
        }

        Text(
          text = stringResource(Res.string.dialog_quick_match_no_review),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(QuickMatchChoice(replaceDetails, replaceCover)) },
        modifier = Modifier.testTag("quick_match_confirm"),
      ) {
        Text(stringResource(Res.string.dialog_quick_match_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(Res.string.dialog_quick_match_cancel))
      }
    },
  )
}

@Composable
private fun ReplaceSwitch(
  checked: Boolean,
  label: String,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  description: String? = null,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
      .padding(vertical = 8.dp),
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
      )
      if (description != null) {
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Switch(checked = checked, onCheckedChange = null)
  }
}
