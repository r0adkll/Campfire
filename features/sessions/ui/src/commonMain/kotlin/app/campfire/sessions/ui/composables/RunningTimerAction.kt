package app.campfire.sessions.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.animated.AnimatedTimerPainter
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_sleep_timer
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource

@Composable
fun RunningTimerAction(
  runningTimer: RunningTimer?,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (runningTimer?.timer !is PlaybackTimer.EndOfChapter) {
    val label = stringResource(Res.string.action_sleep_timer)
    IconButtonTooltip(
      text = label,
      modifier = modifier,
    ) {
      IconButton(
        onClick = onClick,
      ) {
        if (runningTimer != null) {
          Icon(
            AnimatedTimerPainter,
            contentDescription = label,
          )
        } else {
          Icon(
            Icons.Outlined.Timer,
            contentDescription = label,
          )
        }
      }
    }
  } else {
    Box(
      modifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .width(96.dp)
        .height(48.dp)
        .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
    ) {
      val remaining = (currentDuration - currentTime).div(playbackSpeed.toDouble())
      Text(
        text = remaining.thresholdReadoutFormat(),
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
      )
    }
  }
}
