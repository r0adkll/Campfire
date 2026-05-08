package app.campfire.sessions.ui.sheets.description

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.extensions.ReadoutStyle
import app.campfire.common.compose.extensions.asRelativeDayLabel
import app.campfire.common.compose.extensions.linkifyTimestamps
import app.campfire.common.compose.extensions.readoutAtMost
import app.campfire.common.compose.extensions.toRichTextHtml
import app.campfire.common.compose.widgets.WithTimestampUriHandler
import app.campfire.common.compose.widgets.bottomSheetShape
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.logging.bark
import app.campfire.core.model.PodcastEpisode
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.description_bottomsheet_empty_message
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.TokenClickHandler
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.time.Duration
import kotlin.time.DurationUnit
import org.jetbrains.compose.resources.stringResource

suspend fun OverlayHost.showEpisodeDescriptionBottomSheet(
  episode: PodcastEpisode,
  onSeek: (Duration) -> Unit,
) {
  show(
    BottomSheetOverlay(
      model = episode,
      onDismiss = { Unit },
      sheetShape = bottomSheetShape,
    ) { ep, _ ->
      Impression {
        ScreenViewEvent("EpisodeDescription", ScreenType.Overlay)
      }

      SessionSheetLayout(
        title = { Text(episode.title) },
      ) {
        EpisodeDescriptionBottomSheet(
          episode = ep,
          onSeek = onSeek,
        )
      }
    },
  )
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun EpisodeDescriptionBottomSheet(
  episode: PodcastEpisode,
  onSeek: (Duration) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp),
  ) {
    Spacer(Modifier.height(16.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      episode.publishedAtMillis?.let { publishedAt ->
        DescriptionMetadataChip {
          Metadata(
            icon = Icons.Outlined.Today,
            label = publishedAt.asDate().asRelativeDayLabel(ReadoutStyle.Short),
          )
        }
      }

      DescriptionMetadataChip {
        Metadata(
          icon = Icons.Outlined.Schedule,
          label = episode.duration.readoutAtMost(atMost = DurationUnit.MINUTES),
        )
      }

      DescriptionMetadataChip {
        Metadata(
          icon = Icons.Outlined.SdStorage,
          label = episode.sizeInBytes.asReadableBytes(),
        )
      }
    }

    Spacer(Modifier.height(20.dp))

    val description = episode.description?.trim()
    if (description.isNullOrEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(120.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(Res.string.description_bottomsheet_empty_message),
          style = MaterialTheme.typography.bodyLarge,
          fontStyle = FontStyle.Italic,
          textAlign = TextAlign.Center,
          modifier = Modifier.alpha(0.7f),
        )
      }
    } else {
      WithTimestampUriHandler(onSeek = onSeek) {
        RichText(
          state = rememberHtmlRichTextState(description),
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.fillMaxWidth(),
          onTokenClick = TokenClickHandler { token, offset ->
            bark { "Token click[$offset]: ${token.label}" }
          },
        )
      }
    }

    Spacer(Modifier.height(16.dp))
    Spacer(Modifier.navigationBarsPadding())
  }
}

@Composable
private fun rememberHtmlRichTextState(html: String): RichTextState {
  val state = rememberRichTextState()
  LaunchedEffect(html) {
    state.setHtml(html.toRichTextHtml().linkifyTimestamps())
  }
  return state
}

@Composable
private fun DescriptionMetadataChip(
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
  content: @Composable () -> Unit,
) {
  Surface(
    color = containerColor,
    shape = MaterialTheme.shapes.small,
    border = BorderStroke(width = 1.dp, color = borderColor),
    modifier = modifier,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides MaterialTheme.colorScheme.contentColorFor(containerColor),
      LocalTextStyle provides MaterialTheme.typography.labelSmall,
    ) {
      Box(
        Modifier.padding(
          horizontal = 12.dp,
          vertical = 6.dp,
        ),
      ) {
        content()
      }
    }
  }
}

@Composable
private fun Metadata(
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      icon,
      contentDescription = null,
      modifier = Modifier.size(14.dp),
    )
    Spacer(Modifier.width(6.dp))
    Text(
      text = label,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
