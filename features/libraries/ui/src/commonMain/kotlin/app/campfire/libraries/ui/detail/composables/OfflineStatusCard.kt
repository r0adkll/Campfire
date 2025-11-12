package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.extensions.asReadableBytes
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_delete_offline
import campfire.features.libraries.ui.generated.resources.action_stop_downloading
import org.jetbrains.compose.resources.stringResource

@Composable
fun OfflineStatusCard(
  offlineDownload: OfflineDownload,
  onDeleteClick: () -> Unit,
  onStopClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    shape = MaterialTheme.shapes.large,
  ) {
    var expanded by remember { mutableStateOf(!offlineDownload.isCompleted) }

    LaunchedEffect(offlineDownload) {
      if (expanded && offlineDownload.isCompleted) {
        expanded = false
      }
    }

    OfflineTitleBar(
      expanded = expanded,
      completed = offlineDownload.isCompleted,
      onClick = {
        expanded = !expanded
      },
      title = {
        Text(
          when (offlineDownload.state) {
            OfflineDownload.State.None,
            OfflineDownload.State.Downloading,
            -> "Downloading"

            OfflineDownload.State.Queued -> "Queued"
            OfflineDownload.State.Stopped -> "Stopped"
            OfflineDownload.State.Failed -> "Failed"
            OfflineDownload.State.Completed -> "Available for offline"
          },
        )
      },
      modifier = Modifier.testTag("offline_title_bar"),
    )

    AnimatedVisibility(
      visible = expanded,
    ) {
      if (offlineDownload.isCompleted) {
        OfflineCompletedActions(
          sizeInBytes = offlineDownload.contentLength,
          onDeleteClick = onDeleteClick,
        )
      } else {
        OfflineProgressBar(
          progress = offlineDownload.progress.percent,
          bytesDownloaded = offlineDownload.progress.bytes,
          contentLength = offlineDownload.contentLength,
          isIndeterminate = offlineDownload.progress.indeterminate,
          onStopClick = onStopClick,
        )
      }
    }
  }
}

@Composable
private fun OfflineTitleBar(
  expanded: Boolean,
  completed: Boolean,
  onClick: () -> Unit,
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(
        onClick = onClick,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      if (completed) Icons.Rounded.DownloadDone else Icons.Rounded.Downloading,
      contentDescription = null,
      modifier = Modifier
        .padding(16.dp),
    )

    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.CenterStart,
    ) {
      ProvideTextStyle(
        MaterialTheme.typography.titleMedium,
      ) {
        title()
      }
    }

    Spacer(Modifier.width(8.dp))

    val iconRotation by animateFloatAsState(if (expanded) 180f else 0f)
    Icon(
      Icons.Rounded.KeyboardArrowDown,
      contentDescription = null,
      modifier = Modifier
        .padding(16.dp)
        .rotate(iconRotation),
    )
  }
}

@Composable
private fun OfflineProgressBar(
  progress: Float,
  bytesDownloaded: Long,
  contentLength: Long,
  isIndeterminate: Boolean,
  onStopClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .padding(
        start = 16.dp,
        end = 16.dp,
        bottom = 8.dp,
      ),
  ) {
    if (isIndeterminate) {
      LinearProgressIndicator(
        trackColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("indeterminate_progress_bar"),
      )
    } else {
      LinearProgressIndicator(
        progress = { progress },
        trackColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("determinate_progress_bar"),
      )
    }

    Spacer(Modifier.height(4.dp))

    AnimatedVisibility(
      visible = !isIndeterminate,
    ) {
      Row(
        Modifier.fillMaxWidth(),
      ) {
        Text(
          text = bytesDownloaded.asReadableBytes(),
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = contentLength.asReadableBytes(),
          textAlign = TextAlign.End,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.weight(1f),
        )
      }
    }

    Spacer(Modifier.height(4.dp))

    TextButton(
      onClick = onStopClick,
      modifier = Modifier.align(Alignment.End),
    ) {
      Text(stringResource(Res.string.action_stop_downloading))
    }
  }
}

@Composable
private fun OfflineCompletedActions(
  sizeInBytes: Long,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        start = 16.dp,
        end = 16.dp,
        bottom = 16.dp,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = sizeInBytes.asReadableBytes(),
      textAlign = TextAlign.End,
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.weight(1f),
    )

    Spacer(Modifier.width(16.dp))

    Button(
      onClick = onDeleteClick,
      contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
    ) {
      Icon(
        Icons.Rounded.Delete,
        contentDescription = null,
      )
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(stringResource(Res.string.action_delete_offline))
    }
  }
}
