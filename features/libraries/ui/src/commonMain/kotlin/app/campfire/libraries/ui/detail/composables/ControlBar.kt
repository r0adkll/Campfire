package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.MarkFinished
import app.campfire.common.compose.icons.outline.Autoplay
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.core.model.MediaProgress
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_play
import campfire.features.libraries.ui.generated.resources.action_resume_listening
import campfire.features.libraries.ui.generated.resources.menu_item_discard_progress
import campfire.features.libraries.ui.generated.resources.menu_item_mark_finished
import campfire.features.libraries.ui.generated.resources.menu_item_mark_not_finished
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ControlBar(
  mediaProgress: MediaProgress?,
  offlineDownload: OfflineDownload?,
  isCurrentListening: Boolean,
  onPlayClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onMarkFinished: () -> Unit,
  onMarkNotFinished: () -> Unit,
  onDiscardProgress: () -> Unit,
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

      val splitButtonRadius = 4.dp

      Button(
        onClick = onPlayClick,
        enabled = !isCurrentListening,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(
          topStart = CornerSize(50),
          bottomStart = CornerSize(50),
          topEnd = CornerSize(splitButtonRadius),
          bottomEnd = CornerSize(splitButtonRadius),
        ),
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

      Spacer(Modifier.width(2.dp))

      Button(
        enabled = offlineDownload?.state == null || offlineDownload.state == OfflineDownload.State.None,
        onClick = onDownloadClick,
        shape = RoundedCornerShape(
          topStart = CornerSize(splitButtonRadius),
          bottomStart = CornerSize(splitButtonRadius),
          topEnd = CornerSize(50),
          bottomEnd = CornerSize(50),
        ),
        contentPadding = PaddingValues(
          start = 12.dp,
          end = 14.dp,
          top = 8.dp,
          bottom = 8.dp,
        ),
      ) {
        Icon(
          when (offlineDownload?.state) {
            null -> CampfireIcons.Rounded.Download
            OfflineDownload.State.Stopped,
            OfflineDownload.State.None,
            -> CampfireIcons.Rounded.Download

            OfflineDownload.State.Queued,
            OfflineDownload.State.Downloading,
            -> Icons.Rounded.Downloading

            OfflineDownload.State.Failed -> Icons.Rounded.ErrorOutline
            OfflineDownload.State.Completed -> Icons.Rounded.DownloadDone
          },
          contentDescription = null,
          modifier = Modifier.size(22.dp),
        )
      }
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
