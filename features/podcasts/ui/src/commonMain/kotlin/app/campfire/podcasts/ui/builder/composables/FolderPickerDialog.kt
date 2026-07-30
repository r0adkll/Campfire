// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.builder.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.libraries.api.LibraryFolder
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_error_dismiss
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_folder_pick_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FolderPickerDialog(
  folders: List<LibraryFolder>,
  selectedId: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.add_podcast_builder_folder_pick_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        folders.forEach { folder ->
          FolderRow(
            folder = folder,
            isSelected = folder.id == selectedId,
            onClick = {
              onSelect(folder.id)
              onDismiss()
            },
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.add_podcast_builder_error_dismiss))
      }
    },
  )
}

@Composable
private fun FolderRow(
  folder: LibraryFolder,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
  ) {
    RadioButton(selected = isSelected, onClick = onClick)
    Spacer(Modifier.width(8.dp))
    Text(
      text = folder.fullPath,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
