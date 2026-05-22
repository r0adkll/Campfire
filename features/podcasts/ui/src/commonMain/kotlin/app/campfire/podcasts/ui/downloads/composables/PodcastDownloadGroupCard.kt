package app.campfire.podcasts.ui.downloads.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.ui.downloads.DownloadGroup
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.download_queue_clear_action
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PodcastDownloadGroupCard(
  group: DownloadGroup,
  showClearQueue: Boolean,
  onClick: () -> Unit,
  onClearQueueClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    onClick = onClick,
    shape = MaterialTheme.shapes.largeIncreased,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .padding(
          start = 16.dp,
          end = 8.dp,
        ),
    ) {
      Text(
        text = group.podcastTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )

      if (showClearQueue) {
        IconButtonTooltip(text = stringResource(Res.string.download_queue_clear_action)) {
          IconButton(onClick = onClearQueueClick) {
            Icon(
              imageVector = Icons.Outlined.Cancel,
              contentDescription = stringResource(Res.string.download_queue_clear_action),
            )
          }
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    Column(
      verticalArrangement = Arrangement.spacedBy(0.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      group.downloads.forEachIndexed { index, download ->
        RemoteEpisodeDownloadRow(
          download = download,
          modifier = Modifier.fillMaxWidth(),
        )
        if (index != group.downloads.lastIndex) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
              .copy(alpha = 0.4f),
          )
        }
      }
    }
  }
}

// region — Previews —

@Preview
@Composable
private fun PodcastDownloadGroupCardPreview_Admin() = CampfireTheme {
  PodcastDownloadGroupCard(
    group = sampleGroup(
      podcastTitle = "The Ezra Klein Show",
      downloads = listOf(
        sampleDownload(
          id = "d1",
          title = "The view from inside the administration",
          state = RemoteEpisodeDownload.State.Downloading,
        ),
        sampleDownload(id = "d2", title = "What we lose when we lose attention"),
        sampleDownload(id = "d3", title = "Best of: the climate question we keep avoiding"),
      ),
    ),
    showClearQueue = true,
    onClick = {},
    onClearQueueClick = {},
    modifier = Modifier.padding(16.dp),
  )
}

@Preview
@Composable
private fun PodcastDownloadGroupCardPreview_NonAdmin() = CampfireTheme {
  PodcastDownloadGroupCard(
    group = sampleGroup(
      podcastTitle = "The Daily",
      downloads = listOf(
        sampleDownload(
          id = "d1",
          title = "Tuesday, May 21, 2026",
          state = RemoteEpisodeDownload.State.Downloading,
        ),
        sampleDownload(id = "d2", title = "Monday, May 20, 2026"),
      ),
    ),
    showClearQueue = false,
    onClick = {},
    onClearQueueClick = {},
    modifier = Modifier.padding(16.dp),
  )
}

@Preview
@Composable
private fun PodcastDownloadGroupCardPreview_SingleDownloading() = CampfireTheme {
  PodcastDownloadGroupCard(
    group = sampleGroup(
      podcastTitle = "Short Wave",
      downloads = listOf(
        sampleDownload(
          id = "d1",
          title = "Why your dog's gut microbiome matters",
          state = RemoteEpisodeDownload.State.Downloading,
        ),
      ),
    ),
    showClearQueue = true,
    onClick = {},
    onClearQueueClick = {},
    modifier = Modifier.padding(16.dp),
  )
}

@Preview
@Composable
private fun PodcastDownloadGroupCardPreview_LongTitle() = CampfireTheme {
  PodcastDownloadGroupCard(
    group = sampleGroup(
      podcastTitle = "An Extremely Long Podcast Title That Should Wrap Or Ellipsize Gracefully When Rendered",
      downloads = listOf(
        sampleDownload(id = "d1", title = null), // tests "Untitled episode" fallback
        sampleDownload(
          id = "d2",
          title = "An equally long episode title that puts the row's text overflow handling to work",
        ),
      ),
    ),
    showClearQueue = true,
    onClick = {},
    onClearQueueClick = {},
    modifier = Modifier.padding(16.dp),
  )
}

private fun sampleGroup(
  podcastTitle: String,
  downloads: List<RemoteEpisodeDownload>,
) = DownloadGroup(
  libraryItemId = "li_${podcastTitle.hashCode()}",
  podcastTitle = podcastTitle,
  downloads = downloads.toImmutableList(),
)

private fun sampleDownload(
  id: String,
  title: String?,
  state: RemoteEpisodeDownload.State = RemoteEpisodeDownload.State.Queued,
) = RemoteEpisodeDownload(
  id = id,
  libraryItemId = "li_sample",
  libraryId = "lib_podcasts",
  url = "https://feed.example.com/$id.mp3",
  episodeDisplayTitle = title,
  podcastTitle = null,
  state = state,
  createdAt = 0L,
  startedAt = if (state == RemoteEpisodeDownload.State.Downloading) 0L else null,
)

// endregion
