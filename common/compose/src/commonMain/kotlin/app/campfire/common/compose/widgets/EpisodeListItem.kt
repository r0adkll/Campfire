// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.DisabledAlpha
import app.campfire.common.compose.extensions.htmlToPlainText
import app.campfire.common.compose.extensions.relativeDayLabel
import app.campfire.common.compose.extensions.thenIf
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.outline.Autoplay
import app.campfire.common.compose.icons.rounded.FatCheck
import app.campfire.common.compose.icons.rounded.MotionPlay
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.QueuePlayNext
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.swipeaction.AnimatedQueueBackgroundContent
import app.campfire.common.compose.widgets.swipeaction.SwipeActionBox
import app.campfire.core.extensions.asDate
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.mediaProgress
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.action_add_episode_to_queue
import campfire.common.compose.generated.resources.action_remove_episode_from_queue
import campfire.common.compose.generated.resources.duration_finished
import campfire.common.compose.generated.resources.duration_remaining
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.times
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * A podcast episode's row.
 *
 * @param isQueued whether the episode is in the playback queue.
 * @param onQueueChange called with true to add the episode to the queue and false to take it out.
 *   When set, the row can be swiped in the reading direction to add the episode, or take it out if
 *   it's queued, and gets a button that does the same, for those who can't swipe.
 */
@Composable
fun EpisodeListItem(
  episode: PodcastEpisode,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
  mediaProgress: MediaProgress? = null,
  isCurrentSession: Boolean = false,
  isQueued: Boolean = false,
  onQueueChange: ((Boolean) -> Unit)? = null,
  leading: (@Composable () -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
  shape: Shape = MaterialTheme.shapes.medium,
  sessionShape: Shape = MaterialTheme.shapes.largeIncreased,
  colors: EpisodeListItemColors = EpisodeListItemDefaults.colors(),
) {
  if (onQueueChange == null) {
    EpisodeCard(
      episode = episode,
      onClick = onClick,
      onPlayClick = onPlayClick,
      mediaProgress = mediaProgress,
      isCurrentSession = isCurrentSession,
      isQueued = false,
      onQueueChange = null,
      leading = leading,
      actions = actions,
      shape = shape,
      sessionShape = sessionShape,
      colors = colors,
      modifier = modifier,
    )
  } else {
    SwipeActionBox(
      onSwipe = { onQueueChange(!isQueued) },
      backgroundContent = { state ->
        AnimatedQueueBackgroundContent(state = state, isQueued = isQueued)
      },
      modifier = modifier,
    ) {
      EpisodeCard(
        episode = episode,
        onClick = onClick,
        onPlayClick = onPlayClick,
        mediaProgress = mediaProgress,
        isCurrentSession = isCurrentSession,
        isQueued = isQueued,
        onQueueChange = onQueueChange,
        leading = leading,
        actions = actions,
        shape = shape,
        sessionShape = sessionShape,
        colors = colors,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodeCard(
  episode: PodcastEpisode,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  mediaProgress: MediaProgress?,
  isCurrentSession: Boolean,
  isQueued: Boolean,
  onQueueChange: ((Boolean) -> Unit)?,
  leading: (@Composable () -> Unit)?,
  actions: @Composable RowScope.() -> Unit,
  shape: Shape,
  sessionShape: Shape,
  colors: EpisodeListItemColors,
  modifier: Modifier = Modifier,
) {
  val isFinished = mediaProgress?.isFinished == true
  Card(
    modifier = modifier,
    shape = if (isCurrentSession) sessionShape else shape,
    colors = CardDefaults.elevatedCardColors(
      containerColor = when {
        isCurrentSession -> colors.currentSessionContainerColor
        isFinished -> colors.finishedContainerColor
        else -> colors.containerColor
      },
      contentColor = when {
        isCurrentSession -> colors.currentSessionContentColor
        isFinished -> colors.finishedContentColor
        else -> colors.contentColor
      },
    ),
    elevation = CardDefaults.elevatedCardElevation(
      defaultElevation = if (isCurrentSession) 3.dp else 1.dp,
    ),
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
      Row {
        leading?.let { leadingBlock ->
          Box(
            modifier = Modifier
              .size(72.dp)
              .thenIf(isFinished) {
                alpha(DisabledAlpha)
              },
          ) {
            leadingBlock()
          }
          Spacer(Modifier.width(8.dp))
        }

        Row(
          modifier = Modifier
            .fillMaxWidth(),
        ) {
          Text(
            text = episode.title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            modifier = Modifier.weight(1f),
            color = LocalContentColor.current.copy(
              alpha = if (isFinished) DisabledAlpha else 1f,
            ),
          )

          episode.episode?.takeIf { it.isNotBlank() }?.let { ep ->
            Spacer(Modifier.width(8.dp))
            Surface(
              color = if (isFinished) {
                MaterialTheme.colorScheme.surfaceContainerHigh
              } else {
                MaterialTheme.colorScheme.tertiaryContainer
              },
              shape = MaterialTheme.shapes.small,
            ) {
              Text(
                text = "#$ep",
                style = MaterialTheme.typography.labelLarge,
                color = if (isFinished) {
                  MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = DisabledAlpha,
                  )
                } else {
                  MaterialTheme.colorScheme.onTertiaryContainer
                },
                modifier = Modifier
                  .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                  ),
              )
            }
          }
        }
      }

      if (leading != null) {
        Spacer(Modifier.height(8.dp))
      }

      // Two lines of summary: plain text, rather than parsing and laying out the full show notes
      // for every row (those open in the episode sheet)
      val summary = remember(episode.description) {
        episode.description?.htmlToPlainText()?.takeIf { it.isNotEmpty() } ?: "--"
      }
      Text(
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        color = LocalContentColor.current.copy(
          alpha = if (isFinished) DisabledAlpha else 1f,
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(Modifier.height(8.dp))

      EpisodeActionBar(
        duration = episode.duration,
        mediaProgress = mediaProgress,
        isCurrentSession = isCurrentSession,
        publishedAt = episode.publishedAtMillis?.asDate(),
        onPlayClick = onPlayClick,
        isQueued = isQueued,
        onQueueChange = onQueueChange,
        actions = actions,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodeActionBar(
  duration: Duration,
  publishedAt: LocalDate?,
  mediaProgress: MediaProgress?,
  isCurrentSession: Boolean,
  onPlayClick: () -> Unit,
  isQueued: Boolean,
  onQueueChange: ((Boolean) -> Unit)?,
  actions: @Composable RowScope.() -> Unit,
  modifier: Modifier = Modifier,
) {
  val isFinished = mediaProgress?.isFinished == true
  Row(
    modifier = modifier
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (publishedAt != null) {
      MetadataChip(enabled = !isFinished) {
        Text(publishedAt.relativeDayLabel)
      }
    }

    Spacer(Modifier.weight(1f))

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      actions()
      onQueueChange?.let { onChange ->
        QueueButton(
          isQueued = isQueued,
          onClick = { onChange(!isQueued) },
        )
      }
    }

    Spacer(Modifier.width(8.dp))

    val playButtonSize = ButtonDefaults.ExtraSmallContainerHeight
    Button(
      enabled = !isCurrentSession,
      onClick = onPlayClick,
      shapes = ButtonDefaults.shapes(
        shape = ButtonDefaults.squareShape,
        pressedShape = ButtonDefaults.shape,
      ),
      colors = if (isFinished) {
        ButtonDefaults.buttonColors(
          containerColor = CampfireTheme.colorScheme.success,
          contentColor = CampfireTheme.colorScheme.onSuccess,
        )
      } else {
        ButtonDefaults.buttonColors()
      },
      contentPadding = ButtonDefaults.contentPaddingFor(playButtonSize, hasStartIcon = true),
      modifier = Modifier
        .heightIn(playButtonSize),
    ) {
      Icon(
        when {
          isCurrentSession -> CampfireIcons.Rounded.MotionPlay
          mediaProgress != null && !mediaProgress.isFinished -> CampfireIcons.Outline.Autoplay
          isFinished -> CampfireIcons.Rounded.FatCheck
          else -> CampfireIcons.Rounded.PlayArrow
        },
        contentDescription = "Play episode",
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(playButtonSize)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(playButtonSize)))

      val timeLeft = mediaProgress?.actualProgress
        ?.let { progress -> (1f - progress).coerceIn(0f, 1f).toDouble() }
        ?.takeIf { !it.isNaN() }
        ?.let { inverseProgress -> inverseProgress * duration }

      Text(
        text = when {
          isFinished -> stringResource(Res.string.duration_finished)
          timeLeft != null -> stringResource(Res.string.duration_remaining, timeLeft.thresholdReadoutFormat())
          else -> duration.thresholdReadoutFormat()
        },
        style = ButtonDefaults.textStyleFor(playButtonSize),
      )
    }
  }
}

@Composable
private fun QueueButton(
  isQueued: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val label = stringResource(
    if (isQueued) Res.string.action_remove_episode_from_queue else Res.string.action_add_episode_to_queue,
  )
  IconButtonTooltip(text = label) {
    IconButton(
      onClick = onClick,
      modifier = modifier
        .size(
          IconButtonDefaults.extraSmallContainerSize(
            IconButtonDefaults.IconButtonWidthOption.Uniform,
          ),
        ),
      shape = IconButtonDefaults.extraSmallSquareShape,
    ) {
      Icon(
        if (isQueued) CampfireIcons.Filled.QueuePlayNext else CampfireIcons.Rounded.QueuePlayNext,
        contentDescription = label,
        modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
      )
    }
  }
}

@Immutable
class EpisodeListItemColors internal constructor(
  val currentSessionContainerColor: Color,
  val currentSessionContentColor: Color,
  val finishedContainerColor: Color,
  val finishedContentColor: Color,
  val containerColor: Color,
  val contentColor: Color,
) {

  fun copy(
    currentSessionContainerColor: Color = this.currentSessionContainerColor,
    currentSessionContentColor: Color = this.currentSessionContentColor,
    finishedContainerColor: Color = this.finishedContainerColor,
    finishedContentColor: Color = this.finishedContentColor,
    containerColor: Color = this.containerColor,
    contentColor: Color = this.contentColor,
  ) = EpisodeListItemColors(
    currentSessionContainerColor.takeOrElse { this.currentSessionContainerColor },
    currentSessionContentColor.takeOrElse { this.currentSessionContentColor },
    finishedContainerColor.takeOrElse { this.finishedContainerColor },
    finishedContentColor.takeOrElse { this.finishedContentColor },
    containerColor.takeOrElse { this.containerColor },
    contentColor.takeOrElse { this.contentColor },
  )
}

object EpisodeListItemDefaults {

  val largeCornerSize: Dp = 20.dp
  val smallCornerSize: Dp = 4.dp
  val singleCornerSize: Dp = 12.dp

  fun topItemShape(): Shape = RoundedCornerShape(
    topStart = largeCornerSize,
    topEnd = largeCornerSize,
    bottomStart = smallCornerSize,
    bottomEnd = smallCornerSize,
  )

  fun middleItemShape(): Shape = RoundedCornerShape(smallCornerSize)

  fun bottomItemShape(): Shape = RoundedCornerShape(
    topStart = smallCornerSize,
    topEnd = smallCornerSize,
    bottomStart = largeCornerSize,
    bottomEnd = largeCornerSize,
  )

  fun singleItemShape(): Shape = RoundedCornerShape(singleCornerSize)

  @Composable
  fun colors(
    currentSessionContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    currentSessionContentColor: Color = contentColorFor(currentSessionContainerColor),
    finishedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    finishedContentColor: Color = contentColorFor(finishedContainerColor),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
  ): EpisodeListItemColors = EpisodeListItemColors(
    currentSessionContainerColor = currentSessionContainerColor,
    currentSessionContentColor = currentSessionContentColor,
    finishedContainerColor = finishedContainerColor,
    finishedContentColor = finishedContentColor,
    containerColor = containerColor,
    contentColor = contentColor,
  )
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
        episode = mockEpisode(),
      )
    }
  }
}

@Preview
@Composable
fun EpisodeListItemCurrentPreview() {
  CampfireTheme {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      EpisodeListItem(
        onClick = {},
        onPlayClick = {},
        isCurrentSession = true,
        modifier = Modifier.padding(8.dp),
        episode = mockEpisode(),
      )
    }
  }
}

@Preview
@Composable
fun EpisodeListItemProgressPreview() {
  CampfireTheme {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      EpisodeListItem(
        onClick = {},
        onPlayClick = {},
        modifier = Modifier.padding(8.dp),
        mediaProgress = mediaProgress(duration = mockDuration),
        episode = mockEpisode(),
      )
    }
  }
}

@Preview
@Composable
fun EpisodeListItemProgressFinishedPreview() {
  CampfireTheme {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      EpisodeListItem(
        onClick = {},
        onPlayClick = {},
        modifier = Modifier.padding(8.dp),
        mediaProgress = mediaProgress(progress = 1f, duration = mockDuration, isFinished = true),
        episode = mockEpisode(),
      )
    }
  }
}

private val mockDuration = 119.minutes
private fun mockEpisode(): PodcastEpisode {
  return PodcastEpisode(
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
    durationInMillis = mockDuration.inWholeMilliseconds,
    sizeInBytes = 63_830_000,
  )
}
