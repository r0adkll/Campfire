package app.campfire.audioplayer.ui.composables

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.clockFormat
import campfire.infra.audioplayer.public_ui.generated.resources.Res
import campfire.infra.audioplayer.public_ui.generated.resources.timer_end_of_chapter
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RunningTimerText(
  runningTimer: RunningTimer,
  modifier: Modifier = Modifier,
  style: @Composable (PlaybackTimer) -> TextStyle = {
    when (it) {
      is PlaybackTimer.EndOfChapter -> MaterialTheme.typography.displaySmall
      is PlaybackTimer.Epoch -> MaterialTheme.typography.displayMedium
    }
  },
  color: Color = LocalContentColor.current,
  endOfChapterText: String = stringResource(Res.string.timer_end_of_chapter),
) {
  var timeLeft by remember { mutableStateOf("") }
  LaunchedEffect(runningTimer) {
    val timer = (runningTimer.timer as? PlaybackTimer.Epoch) ?: return@LaunchedEffect
    while (isActive) {
      val elapsed = Clock.System.now().toEpochMilliseconds() - runningTimer.startedAt
      val remaining = (timer.epochMillis - elapsed).milliseconds
      timeLeft = remaining.clockFormat()
      delay(1000L)
    }
  }

  Text(
    text = when (runningTimer.timer) {
      is PlaybackTimer.Epoch -> timeLeft
      is PlaybackTimer.EndOfChapter -> endOfChapterText
      else -> ""
    },
    textAlign = TextAlign.Center,
    style = style(runningTimer.timer),
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.ExtraBold,
    color = color,
    modifier = modifier,
  )
}
