package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.relativeDayLabel
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.extensions.toRichTextHtml
import app.campfire.common.compose.icons.rounded.MarkFinished
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.extensions.asDate
import app.campfire.core.model.PodcastEpisode
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.ui.material3.RichText
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun EpisodeListItem(
  episode: PodcastEpisode,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  Card(
    modifier = modifier,
    shape = shape,
    colors = CardDefaults.elevatedCardColors(),
    elevation = CardDefaults.elevatedCardElevation(),
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier
        .padding(
          start = 16.dp,
          end = 16.dp,
          top = 16.dp,
          bottom = 8.dp,
        ),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth(),
      ) {
        Text(
          text = episode.title,
          style = MaterialTheme.typography.titleMediumEmphasized,
          modifier = Modifier.weight(1f),
        )

        episode.episode?.let { ep ->
          Spacer(Modifier.width(8.dp))
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.small,
          ) {
            Text(
              text = "#$ep",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier
                .padding(
                  horizontal = 8.dp,
                  vertical = 4.dp,
                ),
            )
          }
        }
      }

      RichText(
        state = rememberRichTextState(
          episode.description?.toRichTextHtml() ?: "--",
        ),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(Modifier.height(8.dp))

      EpisodeActionBar(
        duration = episode.duration,
        publishedAt = episode.publishedAtMillis?.asDate(),
        onPlayClick = onPlayClick,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodeActionBar(
  duration: Duration,
  publishedAt: LocalDate?,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (publishedAt != null) {
      MetadataChip {
        Text(publishedAt.relativeDayLabel)
      }
    }

    Spacer(Modifier.weight(1f))

    IconButton(
      onClick = {},
      modifier = Modifier
//        .minimumInteractiveComponentSize()
        .size(
          IconButtonDefaults.extraSmallContainerSize(
            IconButtonDefaults.IconButtonWidthOption.Uniform,
          ),
        ),
      shape = IconButtonDefaults.extraSmallSquareShape,
    ) {
      Icon(
        Icons.AutoMirrored.Rounded.PlaylistAdd,
        contentDescription = null,
        modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
      )
    }

    Spacer(Modifier.width(4.dp))

    IconButton(
      onClick = {},
      modifier = Modifier
//        .minimumInteractiveComponentSize()
        .size(
          IconButtonDefaults.extraSmallContainerSize(
            IconButtonDefaults.IconButtonWidthOption.Uniform,
          ),
        ),
      shape = IconButtonDefaults.extraSmallSquareShape,
    ) {
      Icon(
        Icons.Rounded.MarkFinished,
        contentDescription = null,
        modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
      )
    }

    Spacer(Modifier.width(8.dp))

    val playButtonSize = ButtonDefaults.ExtraSmallContainerHeight
    Button(
      onClick = onPlayClick,
      shapes = ButtonDefaults.shapes(
        shape = ButtonDefaults.squareShape,
        pressedShape = ButtonDefaults.shape,
      ),
      contentPadding = ButtonDefaults.contentPaddingFor(playButtonSize, hasStartIcon = true),
      modifier = Modifier
        .heightIn(playButtonSize),
    ) {
      Icon(
        Icons.Rounded.PlayArrow,
        contentDescription = "Play episode",
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(playButtonSize)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(playButtonSize)))
      Text(
        text = duration.thresholdReadoutFormat(),
        style = ButtonDefaults.textStyleFor(playButtonSize),
      )
    }
  }
}

@Preview
@Composable
fun EpisodeListItemPreview() {
  CampfireTheme {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      EpisodeListItem(
        onClick = {},
        onPlayClick = {},
        modifier = Modifier.padding(8.dp),
        episode = PodcastEpisode(
          id = "someid",
          libraryItemId = "someLibraryId",
          podcastId = "someMediaId",
          index = 0,
          season = null,
          episode = "356",
          episodeType = "full",
          title = "The End Is Here s/ Ari Shaffir | 2 Bears, 1 Cave",
          subtitle = null,
          description = "The End is Ari's new storytelling show! And it's finally here! Only at " +
            "https://theend.ymhstudios.com/ . Get 7 full, hour long episodes of completely unfiltered " +
            "stories for \$29.99. Get it now!\n",
          pubDate = null,
          publishedAtMillis = Clock.System.now().toEpochMilliseconds(),
          addedAtMillis = Clock.System.now().toEpochMilliseconds(),
          updatedAtMillis = Clock.System.now().toEpochMilliseconds(),
          durationInMillis = (119.minutes).inWholeMilliseconds,
          sizeInBytes = 63_830_000,
        ),
      )
    }
  }
}
