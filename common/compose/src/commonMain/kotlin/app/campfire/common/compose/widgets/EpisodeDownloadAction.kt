// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.DownloadDone
import app.campfire.offline.Download
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.action_download_episode
import campfire.common.compose.generated.resources.action_episode_download_in_progress
import campfire.common.compose.generated.resources.action_episode_download_queued
import campfire.common.compose.generated.resources.action_remove_episode_download
import org.jetbrains.compose.resources.stringResource

/**
 * An episode row's download button, which doubles as its download status: it starts a download,
 * shows one that is queued or on its way and stops it, or marks a finished one and removes it.
 */
@Composable
fun EpisodeDownloadAction(
  offlineDownload: Download?,
  onDownloadClick: () -> Unit,
  onStopClick: () -> Unit,
  onRemoveClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = offlineDownload?.state

  val label = stringResource(
    when (state) {
      Download.State.Queued -> Res.string.action_episode_download_queued
      Download.State.Downloading -> Res.string.action_episode_download_in_progress
      Download.State.Downloaded -> Res.string.action_remove_episode_download
      null,
      Download.State.Failed,
      Download.State.Stopped,
      -> Res.string.action_download_episode
    },
  )

  val onClick: () -> Unit = when (state) {
    Download.State.Queued,
    Download.State.Downloading,
    -> onStopClick

    Download.State.Downloaded -> onRemoveClick

    null,
    Download.State.Failed,
    Download.State.Stopped,
    -> onDownloadClick
  }

  IconButtonTooltip(text = label, modifier = modifier) {
    IconButton(
      onClick = onClick,
      modifier = Modifier
        .size(
          IconButtonDefaults.extraSmallContainerSize(
            IconButtonDefaults.IconButtonWidthOption.Uniform,
          ),
        ),
      shape = IconButtonDefaults.extraSmallSquareShape,
    ) {
      val iconSize = IconButtonDefaults.extraSmallIconSize
      when (state) {
        Download.State.Queued -> {
          Box(
            modifier = Modifier.size(iconSize),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              modifier = Modifier.size(iconSize),
            )
          }
        }
        Download.State.Downloading -> {
          val downloadProgress = offlineDownload.progress
          Box(
            modifier = Modifier.size(iconSize),
            contentAlignment = Alignment.Center,
          ) {
            if (downloadProgress == null) {
              CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(iconSize),
              )
            } else {
              CircularProgressIndicator(
                progress = { downloadProgress },
                strokeWidth = 2.dp,
                modifier = Modifier.size(iconSize),
              )
            }
          }
        }
        Download.State.Downloaded -> {
          Icon(
            CampfireIcons.Rounded.DownloadDone,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize),
          )
        }
        null,
        Download.State.Failed,
        Download.State.Stopped,
        -> {
          Icon(
            CampfireIcons.Rounded.Download,
            contentDescription = label,
            modifier = Modifier.size(iconSize),
          )
        }
      }
    }
  }
}
