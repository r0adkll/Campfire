package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.outline.Autoplay
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_play
import campfire.features.libraries.ui.generated.resources.action_resume_listening
import campfire.features.libraries.ui.generated.resources.menu_item_add_collection
import campfire.features.libraries.ui.generated.resources.menu_item_add_playlist
import campfire.features.libraries.ui.generated.resources.menu_item_discard_progress
import campfire.features.libraries.ui.generated.resources.menu_item_mark_finished
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ControlBar(
  hasProgress: Boolean,
  isCurrentListening: Boolean,
  onPlayClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onMarkFinished: () -> Unit,
  onDiscardProgress: () -> Unit,
  onAddToPlaylist: () -> Unit,
  onAddToCollection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth(),
  ) {
    val playButtonIcon = if (hasProgress) {
      Icons.Outlined.Autoplay
    } else {
      Icons.Rounded.PlayArrow
    }

    Button(
      onClick = onPlayClick,
      enabled = !isCurrentListening,
      modifier = Modifier.weight(1f),
    ) {
      Icon(playButtonIcon, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text(
        text = if (hasProgress) {
          stringResource(Res.string.action_resume_listening)
        } else {
          stringResource(Res.string.action_play)
        },
      )
    }

    Spacer(Modifier.width(4.dp))

    FilledIconButton(
      onClick = onDownloadClick,
    ) {
      Icon(
        Icons.Rounded.Download,
        contentDescription = null,
      )
    }

    Box {
      var expanded by remember { mutableStateOf(false) }
      FilledIconButton(
        onClick = {
          expanded = true
        },
      ) {
        Icon(
          Icons.Rounded.MoreVert,
          contentDescription = null,
        )
      }

      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        DropdownMenuItem(
          leadingIcon = { Icon(Icons.Rounded.BookmarkAdded, contentDescription = null) },
          text = { Text(stringResource(Res.string.menu_item_mark_finished)) },
          onClick = {
            onMarkFinished()
            expanded = false
          },
        )
        DropdownMenuItem(
          leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null) },
          text = { Text(stringResource(Res.string.menu_item_discard_progress)) },
          onClick = {
            onDiscardProgress()
            expanded = false
          },
        )
        DropdownMenuItem(
          leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
          text = { Text(stringResource(Res.string.menu_item_add_playlist)) },
          onClick = {
            onAddToPlaylist()
            expanded = false
          },
        )
        DropdownMenuItem(
          leadingIcon = { Icon(Icons.Rounded.LibraryAdd, contentDescription = null) },
          text = { Text(stringResource(Res.string.menu_item_add_collection)) },
          onClick = {
            onAddToCollection()
            expanded = false
          },
        )
      }
    }
  }
}
