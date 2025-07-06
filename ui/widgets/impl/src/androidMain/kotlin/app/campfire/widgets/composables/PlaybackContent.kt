package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.core.extensions.fluentIf
import app.campfire.widgets.theme.CampfireGlanceColorScheme
import app.campfire.widgets.theme.LocalContentColorProvider
import kotlin.time.Duration

@Composable
internal fun ConstrainedPlaybackContent(
  title: String,
  subtitle: String,
  playbackState: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  widthSizeClass: WidgetWidthClass,
  modifier: GlanceModifier = GlanceModifier,
  height: Dp = 110.dp,
  backgroundColor: ColorProvider? = GlanceTheme.colors.secondaryContainer,
  contentColor: ColorProvider = GlanceTheme.colors.onSecondaryContainer,
  content: @Composable RowScope.() -> Unit = {
    PlaybackContent(
      title = title,
      subtitle = subtitle,
      playbackState = playbackState,
      currentTime = currentTime,
      currentDuration = currentDuration,
      playbackSpeed = playbackSpeed,
      widthSizeClass = widthSizeClass,
    )
  },
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .wrapContentHeight(),
    contentAlignment = Alignment.BottomStart,
  ) {
    Row(
      modifier = GlanceModifier
        .height(height)
        .fillMaxWidth()
        .fluentIf(backgroundColor != null) {
          background(backgroundColor!!)
        }
        .padding(
          horizontal = if (widthSizeClass == WidgetWidthClass.Expanded) {
            24.dp
          } else {
            8.dp
          },
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalAlignment = if (widthSizeClass == WidgetWidthClass.Expanded) {
        Alignment.Start
      } else {
        Alignment.CenterHorizontally
      },
      content = {
        CompositionLocalProvider(
          LocalContentColorProvider provides contentColor,
        ) {
          content()
        }
      },
    )

    if (currentDuration > Duration.ZERO && widthSizeClass == WidgetWidthClass.Expanded) {
      val progress = currentTime / currentDuration
      LinearProgressIndicator(
        progress = progress.toFloat(),
        color = CampfireGlanceColorScheme.colors.primary,
        backgroundColor = ColorProvider(Color.Black.copy(0.75f)),
        modifier = GlanceModifier
          .fillMaxWidth()
          .height(4.dp),
      )
    }
  }
}

@Composable
internal fun FullPlaybackContent(
  title: String,
  subtitle: String,
  playbackState: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  widthSizeClass: WidgetWidthClass,
  modifier: GlanceModifier = GlanceModifier,
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.BottomStart,
  ) {
    Row(
      modifier = GlanceModifier
        .fillMaxSize()
        .padding(
          horizontal = if (widthSizeClass == WidgetWidthClass.Expanded) {
            24.dp
          } else {
            8.dp
          },
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalAlignment = if (widthSizeClass == WidgetWidthClass.Expanded) {
        Alignment.Start
      } else {
        Alignment.CenterHorizontally
      },
      content = {
        PlaybackContent(
          title = title,
          subtitle = subtitle,
          playbackState = playbackState,
          currentTime = currentTime,
          currentDuration = currentDuration,
          playbackSpeed = playbackSpeed,
          widthSizeClass = widthSizeClass,
        )
      },
    )

    if (currentDuration > Duration.ZERO && widthSizeClass == WidgetWidthClass.Expanded) {
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

@Composable
internal fun RowScope.PlaybackContent(
  title: String,
  subtitle: String,
  playbackState: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  widthSizeClass: WidgetWidthClass,
) = key("playback-content") {
  if (widthSizeClass == WidgetWidthClass.Expanded) {
    PlaybackInfo(
      title = title,
      subtitle = subtitle,
      modifier = GlanceModifier.defaultWeight(),
      supportingText = {
        if (currentDuration > Duration.ZERO) {
          val currentRemainingDuration = (currentDuration - currentTime).div(playbackSpeed.toDouble())
          Text(
            text = currentRemainingDuration.readoutFormat() + " remaining",
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

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val showTimeRemaining = currentDuration > Duration.ZERO &&
      widthSizeClass == WidgetWidthClass.Compact

    if (showTimeRemaining) {
      Spacer(GlanceModifier.height(8.dp))
    }

    PlaybackActions(
      size = widthSizeClass,
      playbackState = playbackState,
    )

    if (showTimeRemaining) {
      Spacer(GlanceModifier.height(8.dp))

      val currentRemainingDuration = (currentDuration - currentTime).div(playbackSpeed.toDouble())
      Text(
        text = currentRemainingDuration.readoutFormat() + " remaining",
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
