package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
      val hasOfflineDownload = offlineDownload?.state != null &&
        offlineDownload.state != OfflineDownload.State.None

      val splitButtonRadius = 4.dp
      val endCornerRadius by animateDpAsState(
        targetValue = if (hasOfflineDownload) 20.dp else splitButtonRadius,
      )

      Button(
        onClick = onPlayClick,
        modifier = Modifier
          .weight(1f)
          .testTag("button_play"),
        shape = RoundedCornerShape(
          topStart = CornerSize(50),
          bottomStart = CornerSize(50),
          topEnd = CornerSize(endCornerRadius),
          bottomEnd = CornerSize(endCornerRadius),
        ),
      ) {
        Icon(
          if (hasProgress) Icons.Outlined.Autoplay else Icons.Rounded.PlayArrow,
          contentDescription = null,
        )
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

      AnimatedVisibility(
        visible = !hasOfflineDownload,
        enter = expandHorizontally(),
        exit = shrinkHorizontally(),
      ) {
        Button(
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
          modifier = Modifier.testTag("button_download"),
        ) {
          Icon(
            CampfireIcons.Rounded.Download,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }

    if (hasProgress) {
      FilledTonalButton(
        onClick = onDiscardProgress,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("button_discard_progress"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.menu_item_discard_progress))
      }
    }

    // Show the mark as (not) finished buttons based on the state
    if (mediaProgress?.isFinished != true) {
      FilledTonalButton(
        onClick = onMarkFinished,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("button_mark_finished"),
      ) {
        Icon(Icons.Rounded.MarkFinished, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.menu_item_mark_finished))
      }
    } else {
      FilledTonalButton(
        onClick = onMarkNotFinished,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("button_mark_not_finished"),
      ) {
        Icon(Icons.Filled.MarkFinished, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(Res.string.menu_item_mark_not_finished))
      }
    }
  }
}
