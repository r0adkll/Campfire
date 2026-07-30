// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.model.Chapter
import app.campfire.sessions.api.QueuedEntry
import kotlin.time.Duration

@Composable
internal fun ActiveWidgetContent(
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
  queue: List<QueuedEntry>?,
  onClick: Action,
  widgetSizeClass: WidgetSizeClass,
  modifier: GlanceModifier = GlanceModifier,
) {
  PlayerWidgetScaffold(
    onClick = onClick,
    modifier = modifier,
    content = {
      when (widgetSizeClass.height) {
        WidgetHeightClass.Single -> SinglePlaybackContent(
          title = title,
          subtitle = subtitle,
          artworkUrl = artworkUrl,
          playbackState = playbackState,
          currentTime = currentTime,
          currentDuration = currentDuration,
          playbackSpeed = playbackSpeed,
          sizeClass = widgetSizeClass,
        )

        WidgetHeightClass.LargeCompact,
        WidgetHeightClass.Compact,
        -> CompactPlaybackContent(
          title = title,
          subtitle = subtitle,
          artworkUrl = artworkUrl,
          playbackState = playbackState,
          currentTime = currentTime,
          currentDuration = currentDuration,
          playbackSpeed = playbackSpeed,
          sleepTimerDuration = sleepTimerDuration,
          runningTimer = runningTimer,
          prevChapter = prevChapter.takeIf { widgetSizeClass.height == WidgetHeightClass.LargeCompact },
          nextChapter = nextChapter.takeIf { widgetSizeClass.height == WidgetHeightClass.LargeCompact },
          sizeClass = widgetSizeClass,
        )

        WidgetHeightClass.Tall,
        WidgetHeightClass.ExtraTall,
        WidgetHeightClass.Expanded,
        -> ExpandedPlaybackContent(
          title = title,
          subtitle = subtitle,
          artworkUrl = artworkUrl,
          playbackState = playbackState,
          currentTime = currentTime,
          currentDuration = currentDuration,
          playbackSpeed = playbackSpeed,
          sleepTimerDuration = sleepTimerDuration,
          runningTimer = runningTimer,
          queue = queue,
          sizeClass = widgetSizeClass,
        )
      }
    },
  )
}
