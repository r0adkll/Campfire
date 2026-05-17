package app.campfire.podcasts.ui.builder.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.libraries.api.LibraryFolder
import app.campfire.podcasts.ui.builder.FoldersState
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_folder_label
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_folder_load_error
import campfire.features.podcasts.ui.generated.resources.add_podcast_retry
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FolderSection(
  title: String,
  foldersState: FoldersState,
  onSelect: (String) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (foldersState) {
    FoldersState.Loading -> Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier,
    ) {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      Spacer(Modifier.width(12.dp))
      Text(
        text = stringResource(Res.string.add_podcast_builder_folder_label),
        style = MaterialTheme.typography.labelLarge,
      )
    }

    FoldersState.Error -> Column(modifier) {
      Text(
        text = stringResource(Res.string.add_podcast_builder_folder_load_error),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
      )
      Spacer(Modifier.height(8.dp))
      TextButton(onClick = onRetry) {
        Text(stringResource(Res.string.add_podcast_retry))
      }
    }

    is FoldersState.Loaded -> FolderPickerRow(
      title = title,
      folders = foldersState.folders,
      selectedId = foldersState.selectedId,
      onSelect = onSelect,
      modifier = modifier,
    )
  }
}

@Composable
private fun FolderPickerRow(
  title: String,
  folders: List<LibraryFolder>,
  selectedId: String,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  @Suppress("LocalVariableName")
  val PathText: @Composable (LibraryFolder) -> Unit = { folder ->
    Text(
      text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
          append(folder.fullPath)
        }
        withStyle(
          SpanStyle(
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
        ) {
          append("/$title")
        }
      },
    )
  }

  if (folders.size <= 1) {
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      modifier = modifier.fillMaxWidth(),
    ) {
      ListItem(
        leadingContent = { Icon(Icons.Rounded.Folder, contentDescription = null) },
        headlineContent = { Text(stringResource(Res.string.add_podcast_builder_folder_label)) },
        supportingContent = {
          PathText(folders.first())
        },
        colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      )
    }
  } else {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Surface(
      onClick = { showPicker = true },
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      modifier = modifier.fillMaxWidth(),
    ) {
      ListItem(
        leadingContent = { Icon(Icons.Rounded.Folder, contentDescription = null) },
        headlineContent = { Text(stringResource(Res.string.add_podcast_builder_folder_label)) },
        supportingContent = {
          folders.find { it.id == selectedId }?.let { folder ->
            PathText(folder)
          }
        },
        colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      )
    }

    if (showPicker) {
      FolderPickerDialog(
        folders = folders,
        selectedId = selectedId,
        onSelect = onSelect,
        onDismiss = { showPicker = false },
      )
    }
  }
}
