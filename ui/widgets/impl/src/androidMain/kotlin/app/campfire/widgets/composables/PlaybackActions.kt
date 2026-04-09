package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width
import app.campfire.audioplayer.AudioPlayer
import app.campfire.widgets.R
import app.campfire.widgets.callbacks.ForwardActionCallback
import app.campfire.widgets.callbacks.PlayPauseActionCallback
import app.campfire.widgets.callbacks.RewindActionCallback
import app.campfire.widgets.callbacks.SkipNextActionCallback
import app.campfire.widgets.callbacks.SkipPreviousActionCallback
import app.campfire.widgets.theme.LocalContentColorProvider
import app.campfire.widgets.theme.compositeOver
import app.campfire.widgets.theme.withAlpha

@Composable
internal fun PlaybackActions(
  size: WidgetWidthClass,
  playbackState: AudioPlayer.State,
  modifier: GlanceModifier = GlanceModifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (size != WidgetWidthClass.Single) {
      GlanceIconButton(
        resourceId = R.drawable.ic_media_replay,
        contentDescription = null,
        onClickActionCallback = RewindActionCallback::class,
        colorFilter = ColorFilter.tint(
          LocalContentColorProvider.current,
        ),
      )

      Spacer(GlanceModifier.width(16.dp))
    }

    if (playbackState == AudioPlayer.State.Initializing || playbackState == AudioPlayer.State.Buffering) {
      CircularProgressIndicator(
        color = LocalContentColorProvider.current,
      )
    } else {
      CircleIconButton(
        imageProvider = ImageProvider(
          when (playbackState) {
            AudioPlayer.State.Initializing -> error("This state should never be reached")
            AudioPlayer.State.Buffering -> error("This state should never be reached")

            AudioPlayer.State.Playing,
            -> R.drawable.ic_media_pause

            AudioPlayer.State.Paused,
            AudioPlayer.State.Disabled,
            AudioPlayer.State.Finished,
            -> R.drawable.ic_media_play
          },
        ),
        contentDescription = null,
        onClick = actionRunCallback(PlayPauseActionCallback::class.java),
      )
    }

    if (size == WidgetWidthClass.Compact || size == WidgetWidthClass.Expanded) {
      Spacer(GlanceModifier.width(16.dp))

      GlanceIconButton(
        resourceId = R.drawable.ic_media_forward,
        contentDescription = null,
        onClickActionCallback = ForwardActionCallback::class,
        colorFilter = ColorFilter.tint(
          LocalContentColorProvider.current,
        ),
      )
    }
  }
}

@Composable
internal fun FullPlaybackActions(
  playbackState: AudioPlayer.State,
  modifier: GlanceModifier = GlanceModifier,
  spacing: Dp = 6.dp,
) {
  Row(
    modifier = modifier
      .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val secondaryButtonContentColor = GlanceTheme.colors.onPrimaryContainer
    val secondaryButtonBackgroundColor =
      GlanceTheme.colors.primaryContainer.withAlpha(0.75f)
        .compositeOver(GlanceTheme.colors.surface.getColor(LocalContext.current))

    CircleIconButton(
      imageProvider = ImageProvider(R.drawable.ic_media_skip_previous),
      contentDescription = null,
      contentColor = secondaryButtonContentColor,
      backgroundColor = secondaryButtonBackgroundColor,
      onClick = actionRunCallback(SkipPreviousActionCallback::class.java),
      modifier = GlanceModifier,
    )

    Spacer(GlanceModifier.width(spacing))

    CircleIconButton(
      imageProvider = ImageProvider(R.drawable.ic_media_replay),
      contentDescription = null,
      contentColor = secondaryButtonContentColor,
      backgroundColor = secondaryButtonBackgroundColor,
      onClick = actionRunCallback(RewindActionCallback::class.java),
      modifier = GlanceModifier,
    )

    Spacer(GlanceModifier.width(spacing))

    if (playbackState == AudioPlayer.State.Initializing || playbackState == AudioPlayer.State.Buffering) {
      CircularProgressIndicator(
        color = LocalContentColorProvider.current,
      )
    } else {
      SquareIconButton(
        imageProvider = ImageProvider(
          when (playbackState) {
            AudioPlayer.State.Initializing -> error("This state should never be reached")
            AudioPlayer.State.Buffering -> error("This state should never be reached")

            AudioPlayer.State.Playing,
            -> R.drawable.ic_media_pause

            AudioPlayer.State.Paused,
            AudioPlayer.State.Disabled,
            AudioPlayer.State.Finished,
            -> R.drawable.ic_media_play
          },
        ),
        contentDescription = null,
        onClick = actionRunCallback(PlayPauseActionCallback::class.java),
      )
    }

    Spacer(GlanceModifier.width(spacing))

    CircleIconButton(
      imageProvider = ImageProvider(R.drawable.ic_media_forward),
      contentDescription = null,
      contentColor = secondaryButtonContentColor,
      backgroundColor = secondaryButtonBackgroundColor,
      onClick = actionRunCallback(ForwardActionCallback::class.java),
      modifier = GlanceModifier,
    )

    Spacer(GlanceModifier.width(spacing))

    CircleIconButton(
      imageProvider = ImageProvider(R.drawable.ic_media_skip_next),
      contentDescription = null,
      contentColor = secondaryButtonContentColor,
      backgroundColor = secondaryButtonBackgroundColor,
      onClick = actionRunCallback(SkipNextActionCallback::class.java),
      modifier = GlanceModifier,
    )
  }
}
