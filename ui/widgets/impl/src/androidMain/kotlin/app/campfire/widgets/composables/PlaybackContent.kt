// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.composables

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.OutlineButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.core.extensions.readableHundredths
import app.campfire.core.model.Chapter
import app.campfire.sessions.api.QueuedEntry
import app.campfire.widgets.R
import app.campfire.widgets.callbacks.CycleSpeedActionCallback
import app.campfire.widgets.callbacks.SkipNextActionCallback
import app.campfire.widgets.callbacks.SkipPreviousActionCallback
import app.campfire.widgets.theme.LocalContentColorProvider
import app.campfire.widgets.theme.withAlpha
import app.campfire.widgets.util.glanceStringResource
import kotlin.time.Duration

@SuppressLint("RestrictedApi")
@Composable
internal fun SinglePlaybackContent(
  title: String,
  subtitle: String,
  artworkUrl: String?,
  playbackState: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  sizeClass: WidgetSizeClass,
  modifier: GlanceModifier = GlanceModifier,
  showPlaybackActions: Boolean = true,
  defaultBackground: ImageProvider = ImageProvider(R.drawable.default_background),
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.BottomStart,
  ) {
    if (artworkUrl != null) {
      GlanceImage(
        url = artworkUrl,
        modifier = GlanceModifier
          .fillMaxSize(),
        colorFilter = ColorFilter.tint(
          GlanceTheme.colors.secondary.withAlpha(0.75f),
        ),
      )
    } else {
      Image(
        provider = defaultBackground,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        colorFilter = ColorFilter.tint(
          GlanceTheme.colors.secondary.withAlpha(0.5f),
        ),
        modifier = GlanceModifier.fillMaxSize(),
      )
    }

    // Playback Info + Actions
    Row(
      modifier = GlanceModifier
        .fillMaxSize()
        .padding(
          horizontal = if (sizeClass.width == WidgetWidthClass.Expanded) {
            24.dp
          } else {
            8.dp
          },
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalAlignment = if (sizeClass.width == WidgetWidthClass.Expanded) {
        Alignment.Start
      } else {
        Alignment.CenterHorizontally
      },
    ) {
      PlaybackContentRow(
        title = title,
        subtitle = subtitle,
        playbackState = playbackState,
        currentTime = currentTime,
        currentDuration = currentDuration,
        playbackSpeed = playbackSpeed,
        sizeClass = sizeClass,
        showPlaybackActions = showPlaybackActions,
      )
    }

    if (currentDuration > Duration.ZERO && sizeClass.width == WidgetWidthClass.Expanded) {
      val progress = currentTime / currentDuration
      LinearProgressIndicator(
        progress = progress.toFloat(),
        color = GlanceTheme.colors.primaryContainer,
        backgroundColor = ColorProvider(Color.Black.copy(0.5f)),
        modifier = GlanceModifier
          .fillMaxWidth()
          .height(4.dp),
      )
    }
  }
}

@SuppressLint("RestrictedApi")
@Composable
internal fun CompactPlaybackContent(
  title: String,
  subtitle: String,
  artworkUrl: String?,
  playbackState: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  sleepTimerDuration: Duration,
  runningTimer: RunningTimer?,
  prevChapter: Chapter?,
  nextChapter: Chapter?,
  sizeClass: WidgetSizeClass,
  modifier: GlanceModifier = GlanceModifier,
  defaultBackground: ImageProvider = ImageProvider(R.drawable.default_background),
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.BottomStart,
  ) {
    if (artworkUrl != null) {
      GlanceImage(
        url = artworkUrl,
        modifier = GlanceModifier
          .fillMaxSize(),
        colorFilter = ColorFilter.tint(
          GlanceTheme.colors.secondary.withAlpha(0.75f),
        ),
      )
    } else {
      Image(
        provider = defaultBackground,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        colorFilter = ColorFilter.tint(
          GlanceTheme.colors.secondary.withAlpha(0.5f),
        ),
        modifier = GlanceModifier.fillMaxSize(),
      )
    }

    Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .padding(
          horizontal = if (sizeClass.width == WidgetWidthClass.Expanded) {
            24.dp
          } else {
            8.dp
          },
        ),
    ) {
      val playbackInfoModifier = if (sizeClass.height == WidgetHeightClass.Compact) {
        GlanceModifier.defaultWeight()
      } else {
        GlanceModifier.height(96.dp)
      }

      // Playback Info + Actions
      Row(
        modifier = GlanceModifier
          .fillMaxWidth()
          .then(playbackInfoModifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = if (sizeClass.width == WidgetWidthClass.Expanded) {
          Alignment.Start
        } else {
          Alignment.CenterHorizontally
        },
      ) {
        PlaybackContentRow(
          title = title,
          subtitle = subtitle,
          playbackState = playbackState,
          currentTime = currentTime,
          currentDuration = currentDuration,
          playbackSpeed = playbackSpeed,
          sizeClass = sizeClass,
        )
      }

      // Controls
      val controlsModifier = if (sizeClass.height == WidgetHeightClass.Compact) {
        GlanceModifier.padding(vertical = 24.dp)
      } else {
        GlanceModifier
      }
      Row(
        modifier = GlanceModifier
          .fillMaxWidth()
          .then(controlsModifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
      ) {
        OutlineButton(
          text = "${playbackSpeed.readableHundredths}x",
          icon = ImageProvider(R.drawable.ic_media_playbackspeed),
          onClick = actionRunCallback(CycleSpeedActionCallback::class.java),
          contentColor = LocalContentColorProvider.current,
        )

        Spacer(GlanceModifier.width(8.dp))

        SleepTimerButton(
          sleepTimerDuration = sleepTimerDuration,
          runningTimer = runningTimer,
        )
      }

      // Chapters
      if (prevChapter != null || nextChapter != null) {
        Column(
          modifier = GlanceModifier
            .fillMaxWidth()
            .defaultWeight(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalAlignment = Alignment.Start,
        ) {
          if (prevChapter != null) {
            OutlineButton(
              text = prevChapter.title,
              maxLines = 1,
              icon = ImageProvider(R.drawable.ic_media_chevron_left),
              onClick = actionRunCallback(SkipPreviousActionCallback::class.java),
              contentColor = LocalContentColorProvider.current,
              modifier = GlanceModifier
                .fillMaxWidth(),
            )
          }

          if (nextChapter != null) {
            if (prevChapter != null) {
              Spacer(GlanceModifier.height(8.dp))
            }

            OutlineButton(
              text = nextChapter.title,
              maxLines = 1,
              icon = ImageProvider(R.drawable.ic_media_chevron_right),
              onClick = actionRunCallback(SkipNextActionCallback::class.java),
              contentColor = LocalContentColorProvider.current,
              modifier = GlanceModifier
                .fillMaxWidth(),
            )
          }
        }
      }
    }

    if (currentDuration > Duration.ZERO && sizeClass.width == WidgetWidthClass.Expanded) {
      val progress = currentTime / currentDuration
      LinearProgressIndicator(
        progress = progress.toFloat(),
        color = GlanceTheme.colors.primaryContainer,
        backgroundColor = ColorProvider(Color.Black.copy(0.5f)),
        modifier = GlanceModifier
          .fillMaxWidth()
          .height(4.dp),
      )
    }
  }
}

private val ExpandedEdgePadding = 24.dp

@SuppressLint("RestrictedApi")
@Composable
internal fun ExpandedPlaybackContent(
  title: String,
  subtitle: String,
  artworkUrl: String?,
  playbackState: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  sleepTimerDuration: Duration,
  runningTimer: RunningTimer?,
  queue: List<QueuedEntry>?,
  sizeClass: WidgetSizeClass,
  modifier: GlanceModifier = GlanceModifier,
  defaultBackground: ImageProvider = ImageProvider(R.drawable.default_background),
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(
        top = ExpandedEdgePadding,
        bottom = 16.dp,
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val thumbnailFactor = when (sizeClass.height) {
      WidgetHeightClass.Expanded -> 2.25f
      WidgetHeightClass.Tall -> 1.75f
      WidgetHeightClass.ExtraTall -> 1.5f
      else -> 1f
    }
    val artSize = LocalSize.current.width / thumbnailFactor

    // Album Art
    if (artworkUrl != null) {
      GlanceImage(
        url = artworkUrl,
        modifier = GlanceModifier
          .size(artSize)
          .cornerRadius(20.dp),
      )
    } else {
      Image(
        provider = defaultBackground,
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = GlanceModifier
          .size(artSize)
          .cornerRadius(20.dp),
      )
    }

    Spacer(GlanceModifier.height(16.dp))

    Column(
      modifier = GlanceModifier
        .fillMaxWidth()
        .defaultWeight(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(GlanceModifier.defaultWeight())

      // Title / Subtitle
      Text(
        text = title,
        style = TextStyle(
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
        ),
        maxLines = 2,
      )
      Text(
        text = subtitle,
        style = TextStyle(
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
        ),
      )

      if (currentDuration > Duration.ZERO) {
        val progress = currentTime / currentDuration
        LinearProgressIndicator(
          progress = progress.toFloat(),
          color = GlanceTheme.colors.primary,
          backgroundColor = GlanceTheme.colors.surface,
          modifier = GlanceModifier
            .fillMaxWidth()
            .padding(
              horizontal = ExpandedEdgePadding,
              vertical = 16.dp,
            ),
        )
      } else {
        Spacer(GlanceModifier.height(16.dp))
      }

      // Controls
      FullPlaybackActions(
        playbackState = playbackState,
        modifier = GlanceModifier,
      )

      // Additional controls
      if (sizeClass.height >= WidgetHeightClass.Tall) {
        Spacer(GlanceModifier.defaultWeight())
        Row(
          modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = ExpandedEdgePadding),
          verticalAlignment = Alignment.CenterVertically,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          OutlineButton(
            text = "${playbackSpeed.readableHundredths}x",
            icon = ImageProvider(R.drawable.ic_media_playbackspeed),
            onClick = actionRunCallback(CycleSpeedActionCallback::class.java),
            contentColor = GlanceTheme.colors.onPrimaryContainer,
            modifier = GlanceModifier.defaultWeight(),
          )

          Spacer(GlanceModifier.width(8.dp))

          SleepTimerButton(
            sleepTimerDuration = sleepTimerDuration,
            runningTimer = runningTimer,
            modifier = GlanceModifier.defaultWeight(),
            inactiveColor = GlanceTheme.colors.onPrimaryContainer,
            containerColor = GlanceTheme.colors.secondaryContainer,
            contentColor = GlanceTheme.colors.onSecondaryContainer,
          )
        }
        Spacer(GlanceModifier.height(8.dp))
      }

      // Queue
      if (!queue.isNullOrEmpty()) {
        QueueItem(
          queue = queue,
          modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = ExpandedEdgePadding),
        )
        Spacer(GlanceModifier.height(8.dp))
      }
    }
  }
}

@Composable
private fun QueueItem(
  queue: List<QueuedEntry>,
  modifier: GlanceModifier = GlanceModifier,
) {
  Column(
    modifier = modifier,
  ) {
    Text(
      text = glanceStringResource(R.string.player_widget_up_next),
      style = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
      ),
      modifier = GlanceModifier
        .padding(
          vertical = 8.dp,
        ),
    )

    val nextEntry = queue.first()
    val title = nextEntry.episode?.title ?: nextEntry.libraryItem.media.metadata.title ?: "--"
    val duration = nextEntry.episode?.duration ?: nextEntry.libraryItem.media.duration

    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Thumbnail
      val thumbSize = 64.dp
      GlanceImage(
        url = nextEntry.libraryItem.media.coverImageUrl,
        modifier = GlanceModifier
          .size(thumbSize)
          .cornerRadius(16.dp),
      )

      Spacer(GlanceModifier.size(16.dp))

      // Description
      Column(
        modifier = GlanceModifier
          .defaultWeight(),
      ) {
        Text(
          text = title,
          style = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
          ),
          maxLines = 2,
        )

        Text(
          text = nextEntry.libraryItem.media.metadata.authorName ?: "--",
          style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
          ),
          maxLines = 1,
        )

        Text(
          text = duration.readoutFormat(),
          style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
          ),
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
internal fun RowScope.PlaybackContentRow(
  title: String,
  subtitle: String,
  playbackState: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  sizeClass: WidgetSizeClass,
  showPlaybackActions: Boolean = true,
) = key("playback-content") {
  if (sizeClass.width == WidgetWidthClass.Expanded) {
    PlaybackInfo(
      title = title,
      subtitle = subtitle,
      modifier = GlanceModifier.defaultWeight(),
      supportingText = {
        if (currentDuration > Duration.ZERO) {
          val currentRemainingDuration = (currentDuration - currentTime).div(playbackSpeed.toDouble())
          Text(
            text = currentRemainingDuration.readoutFormat(largestOnly = true) + " remaining",
            style = TextStyle(
              color = LocalContentColorProvider.current,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
            ),
            modifier = GlanceModifier.padding(top = 4.dp),
          )
        }
      },
    )
    Spacer(GlanceModifier.width(16.dp))
  }

  if (showPlaybackActions) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val showTimeRemaining = currentDuration > Duration.ZERO &&
        sizeClass.width == WidgetWidthClass.Compact

      if (showTimeRemaining) {
        Spacer(GlanceModifier.height(8.dp))
      }

      PlaybackActions(
        size = sizeClass.width,
        playbackState = playbackState,
      )

      if (showTimeRemaining) {
        Spacer(GlanceModifier.height(8.dp))

        val currentRemainingDuration = (currentDuration - currentTime).div(playbackSpeed.toDouble())
        Text(
          text = currentRemainingDuration.readoutFormat(largestOnly = true) + " remaining",
          style = TextStyle(
            color = LocalContentColorProvider.current,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
          ),
          modifier = GlanceModifier.fillMaxWidth(),
        )
      }
    }
  }
}
