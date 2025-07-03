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
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.animated.AnimatedTimerPainter
import kotlin.time.Duration

@Composable
fun RunningTimerAction(
  runningTimer: RunningTimer?,
  currentTime: Duration,
  currentDuration: Duration,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (runningTimer?.timer !is PlaybackTimer.EndOfChapter) {
    IconButton(
      onClick = onClick,
      modifier = modifier,
    ) {
      if (runningTimer != null) {
        Icon(
          AnimatedTimerPainter,
          contentDescription = null,
        )
      } else {
        Icon(
          Icons.Outlined.Timer,
          contentDescription = null,
        )
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
      val remaining = currentDuration - currentTime

      Text(
        text = remaining.readoutFormat(largestOnly = true),
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
      )
    }
  }
}
