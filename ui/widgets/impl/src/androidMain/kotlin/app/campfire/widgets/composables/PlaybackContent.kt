package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.widgets.theme.LocalContentColorProvider
import kotlin.time.Duration

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
