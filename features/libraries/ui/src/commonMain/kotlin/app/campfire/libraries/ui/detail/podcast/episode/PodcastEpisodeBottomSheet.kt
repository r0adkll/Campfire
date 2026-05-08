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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.ReadoutStyle
import app.campfire.common.compose.extensions.asRelativeDayLabel
import app.campfire.common.compose.extensions.linkifyTimestamps
import app.campfire.common.compose.extensions.readoutAtMost
import app.campfire.common.compose.extensions.toRichTextHtml
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.common.compose.widgets.WithTimestampUriHandler
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.SessionUiState
import app.campfire.libraries.ui.detail.composables.ExpressiveControlBar
import app.campfire.libraries.ui.detail.composables.MediaProgressBar
import app.campfire.libraries.ui.detail.composables.MetadataChip
import app.campfire.libraries.ui.detail.composables.rememberRichTextState
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.TokenClickHandler
import com.mohamedrejeb.richeditor.ui.material.RichText
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.time.DurationUnit

@ContributesTo(UserScope::class)
interface PodcastEpisodeBottomSheetComponent {
  val podcastEpisodePresenterFactory: PodcastEpisodePresenterFactory
}

suspend fun OverlayHost.showPodcastEpisodeBottomSheet(
  episode: PodcastEpisode,
) {
  show(
    BottomSheetOverlay(
      model = episode,
      onDismiss = { Unit },
    ) { podcastEpisode, navigator ->
      CompositionLocalProvider(
        LocalContentLayout provides ContentLayout.Root,
      ) {
        val presenter = remember {
          ComponentHolder.component<PodcastEpisodeBottomSheetComponent>()
            .podcastEpisodePresenterFactory(podcastEpisode, navigator)
        }

        val state = presenter.present()

        PodcastEpisodeBottomSheet(
          state = state,
        )
      }
    },
  )
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun PodcastEpisodeBottomSheet(
  state: PodcastEpisodeUiState,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
  ) {
    // Title
    Text(
      text = state.episode.title,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(16.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      state.episode.publishedAtMillis?.let { publishedAt ->
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
          label = state.episode.duration.readoutAtMost(atMost = DurationUnit.MINUTES),
        )
      }

      MetadataChip {
        Metadata(
          icon = Icons.Outlined.SdStorage,
          label = state.episode.sizeInBytes.asReadableBytes(),
        )
      }
    }

    // Progress bar
    state.progress?.let { progress ->
      Spacer(Modifier.height(24.dp))
      MediaProgressBar(
        isPlaying = state.isPlaying,
        playbackSpeed = state.playbackSpeed,
        totalDuration = state.episode.duration,
        progress = progress,
      )
    }

    Spacer(Modifier.height(24.dp))

    // Controls
    ExpressiveControlBar(
      isQueued = state.isQueued,
      hasSession = state.hasSession,
      isCurrentSession = state.sessionState is SessionUiState.Current,
      mediaProgress = state.progress,
      offlineDownload = null,
      onPlayClick = { state.eventSink(PodcastEpisodeUiEvent.PlayClick) },
      onDownloadClick = { },
      onMarkFinished = { state.eventSink(PodcastEpisodeUiEvent.MarkFinished) },
      onMarkNotFinished = { state.eventSink(PodcastEpisodeUiEvent.MarkNotFinished) },
      onDiscardProgress = { state.eventSink(PodcastEpisodeUiEvent.DiscardProgress) },
      onStopDownloadClick = {},
      onDeleteDownloadClick = {},
      onAddToPlaylistClick = {},
      onAddToQueueClick = {},
    )

    Spacer(Modifier.height(16.dp))

    // Description
    state.episode.description?.let { desc ->
      MetadataHeader(
        title = "Summary",
        textStyle = MaterialTheme.typography.titleLarge,
        textColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .heightIn(min = 56.dp),
      )
      Spacer(Modifier.height(8.dp))

      WithTimestampUriHandler(
        onSeek = { position -> state.eventSink(PodcastEpisodeUiEvent.Seek(position)) },
      ) {
        RichText(
          state = rememberRichTextState(desc.toRichTextHtml().linkifyTimestamps()),
          style = MaterialTheme.typography.bodyLarge,
          onTokenClick = TokenClickHandler { token, offset ->
          },
        )
      }
    }

    // Episode Metadata

    Spacer(
      Modifier.navigationBarsPadding(),
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
