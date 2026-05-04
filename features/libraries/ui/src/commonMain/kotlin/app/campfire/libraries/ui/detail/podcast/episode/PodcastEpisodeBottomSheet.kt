package app.campfire.libraries.ui.detail.podcast.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.ReadoutStyle
import app.campfire.common.compose.extensions.asRelativeDayLabel
import app.campfire.common.compose.extensions.readoutAtMost
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.composables.ExpressiveControlBar
import app.campfire.libraries.ui.detail.composables.MetadataChip
import app.campfire.libraries.ui.detail.composables.rememberRichTextState
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.TokenClickHandler
import com.mohamedrejeb.richeditor.ui.material.RichText
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.time.DurationUnit

suspend fun OverlayHost.showPodcastEpisodeBottomSheet(
  episode: PodcastEpisode,
) {
  show(
    BottomSheetOverlay(
      model = episode,
      onDismiss = { Unit },
    ) { podcastEpisode, navigator ->
      CompositionLocalProvider(
        LocalContentLayout provides ContentLayout.Root
      ) {
        PodcastEpisodeBottomSheet(
          episode = podcastEpisode,
        )
      }
    },
  )
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun PodcastEpisodeBottomSheet(
  episode: PodcastEpisode,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
  ) {

    // Title
    Text(
      text = episode.title,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(16.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      episode.publishedAtMillis?.let { publishedAt ->
        MetadataChip {
          Metadata(
            icon = Icons.Outlined.Today,
            label = publishedAt.asDate().asRelativeDayLabel(ReadoutStyle.Short),
          )
        }
      }

      MetadataChip {
        Metadata(
          icon = Icons.Outlined.Schedule,
          label = episode.duration.readoutAtMost(atMost = DurationUnit.MINUTES),
        )
      }

      MetadataChip {
        Metadata(
          icon = Icons.Outlined.SdStorage,
          label = episode.sizeInBytes.asReadableBytes(),
        )
      }
    }

    Spacer(Modifier.height(24.dp))

    // Controls
    ExpressiveControlBar(
      isQueued = false,
      hasSession = false,
      isCurrentSession = false,
      mediaProgress = null,
      offlineDownload = null,
      onPlayClick = {},
      onDownloadClick = {},
      onMarkFinished = {},
      onMarkNotFinished = {},
      onDiscardProgress = {},
      onStopDownloadClick = {},
      onDeleteDownloadClick = {},
      onAddToPlaylistClick = {},
      onAddToQueueClick = {},
    )

    Spacer(Modifier.height(16.dp))

    // Description
    episode.description?.let { desc ->
      MetadataHeader(
        title = "Summary",
        textStyle = MaterialTheme.typography.titleLarge,
        textColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .heightIn(min = 56.dp),
      )
      Spacer(Modifier.height(8.dp))

      RichText(
        state = rememberRichTextState(desc),
        style = MaterialTheme.typography.bodyLarge,
        onTokenClick = TokenClickHandler { token, offset ->

        },
      )
    }

    // Episode Metadata

    Spacer(
      Modifier.navigationBarsPadding()
    )
  }
}

@Composable
private fun Metadata(
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .alpha(0.65f),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      icon,
      contentDescription = null,
      modifier = Modifier.size(18.dp),
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
