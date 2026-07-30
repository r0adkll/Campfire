// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.downloads.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Downloading
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.podcasts.api.RemoteEpisodeDownload
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.download_queue_state_downloading
import campfire.features.podcasts.ui.generated.resources.download_queue_state_queued
import campfire.features.podcasts.ui.generated.resources.download_queue_untitled_episode
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RemoteEpisodeDownloadRow(
  download: RemoteEpisodeDownload,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    StateIndicator(download.state)
    Column(
      verticalArrangement = Arrangement.spacedBy(2.dp),
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = download.episodeDisplayTitle?.takeIf { it.isNotBlank() }
          ?: stringResource(Res.string.download_queue_untitled_episode),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = stringResource(download.state.label()),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun StateIndicator(
  state: RemoteEpisodeDownload.State,
  modifier: Modifier = Modifier,
) {
  when (state) {
    RemoteEpisodeDownload.State.Downloading -> Box(
      modifier = modifier.size(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 3.dp,
      )
    }

    RemoteEpisodeDownload.State.Queued -> Icon(
      CampfireIcons.Rounded.Downloading,
      contentDescription = stringResource(Res.string.download_queue_state_queued),
    )
  }
}

@Composable
private fun RemoteEpisodeDownload.State.label() = when (this) {
  RemoteEpisodeDownload.State.Downloading -> Res.string.download_queue_state_downloading
  RemoteEpisodeDownload.State.Queued -> Res.string.download_queue_state_queued
}

// region — Previews —

@Preview
@Composable
private fun RemoteEpisodeDownloadRowPreview_Downloading() = CampfireTheme {
  Surface {
    RemoteEpisodeDownloadRow(
      download = sampleRowDownload(
        title = "The view from inside the administration",
        state = RemoteEpisodeDownload.State.Downloading,
      ),
    )
  }
}

@Preview
@Composable
private fun RemoteEpisodeDownloadRowPreview_Queued() = CampfireTheme {
  Surface {
    RemoteEpisodeDownloadRow(
      download = sampleRowDownload(
        title = "Tuesday, May 21, 2026",
        state = RemoteEpisodeDownload.State.Queued,
      ),
    )
  }
}

@Preview
@Composable
private fun RemoteEpisodeDownloadRowPreview_Untitled() = CampfireTheme {
  // Null title falls back to "Untitled episode" via the stringResource path.
  Surface {
    RemoteEpisodeDownloadRow(
      download = sampleRowDownload(
        title = null,
        state = RemoteEpisodeDownload.State.Queued,
      ),
    )
  }
}

@Preview
@Composable
private fun RemoteEpisodeDownloadRowPreview_LongTitle() = CampfireTheme {
  Surface {
    RemoteEpisodeDownloadRow(
      download = sampleRowDownload(
        title = "An extraordinarily lengthy episode title that should ellipsize after two lines " +
          "of text since the row caps at maxLines = 2",
        state = RemoteEpisodeDownload.State.Downloading,
      ),
    )
  }
}

private fun sampleRowDownload(
  title: String?,
  state: RemoteEpisodeDownload.State,
) = RemoteEpisodeDownload(
  id = "d_preview",
  libraryItemId = "li_preview",
  libraryId = "lib_podcasts",
  url = "https://feed.example.com/d_preview.mp3",
  episodeDisplayTitle = title,
  podcastTitle = "Test Podcast",
  state = state,
  createdAt = 0L,
  startedAt = if (state == RemoteEpisodeDownload.State.Downloading) 0L else null,
)

// endregion
