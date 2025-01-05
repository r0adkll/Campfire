package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.filled.MarkFinished
import app.campfire.common.compose.icons.outline.Autoplay
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.core.model.MediaProgress
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_play
import campfire.features.libraries.ui.generated.resources.action_resume_listening
import campfire.features.libraries.ui.generated.resources.menu_item_add_collection
import campfire.features.libraries.ui.generated.resources.menu_item_add_playlist
import campfire.features.libraries.ui.generated.resources.menu_item_discard_progress
import campfire.features.libraries.ui.generated.resources.menu_item_download
import campfire.features.libraries.ui.generated.resources.menu_item_mark_finished
import campfire.features.libraries.ui.generated.resources.menu_item_mark_not_finished
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ControlBar(
  mediaProgress: MediaProgress?,
  isCurrentListening: Boolean,
  onPlayClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onMarkFinished: () -> Unit,
  onMarkNotFinished: () -> Unit,
  onDiscardProgress: () -> Unit,
  onAddToPlaylist: () -> Unit,
  onAddToCollection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hasProgress = mediaProgress != null &&
    mediaProgress.progress > 0f &&
    !mediaProgress.isFinished

  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
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

      ControlsDropdownButton(
        onDownloadClick = onDownloadClick,
        onAddToPlaylist = onAddToPlaylist,
        onAddToCollection = onAddToCollection,
      )
    }

    if (hasProgress) {
      FilledTonalButton(
        onClick = onDiscardProgress,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.menu_item_discard_progress))
      }
      FilledTonalButton(
        onClick = onMarkFinished,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.Rounded.MarkFinished, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.menu_item_mark_finished))
      }
    }

    if (mediaProgress?.isFinished == true) {
      FilledTonalButton(
        onClick = onMarkNotFinished,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.Filled.MarkFinished, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.menu_item_mark_not_finished))
      }
    }
  }
}

@Composable
private fun ControlsDropdownButton(
  onDownloadClick: () -> Unit,
  onAddToPlaylist: () -> Unit,
  onAddToCollection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    var expanded by remember { mutableStateOf(false) }
    FilledTonalIconButton(
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
        leadingIcon = { Icon(Icons.Rounded.DownloadForOffline, contentDescription = null) },
        text = { Text(stringResource(Res.string.menu_item_download)) },
        onClick = {
          onDownloadClick()
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
