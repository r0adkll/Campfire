package app.campfire.widgets.composables

import android.annotation.SuppressLint
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.OutlineButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.widgets.R
import app.campfire.widgets.callbacks.ClearSleepTimerActionCallback
import app.campfire.widgets.callbacks.SleepTimerActionCallback
import app.campfire.widgets.theme.LocalContentColorProvider
import kotlin.time.Duration

@SuppressLint("RestrictedApi")
@Composable
internal fun SleepTimerButton(
  sleepTimerDuration: Duration,
  runningTimer: RunningTimer?,
  modifier: GlanceModifier = GlanceModifier,
  inactiveColor: ColorProvider = LocalContentColorProvider.current,
  containerColor: ColorProvider = LocalContentColorProvider.current,
  contentColor: ColorProvider = GlanceTheme.colors.onSecondaryContainer,
) {
  if (runningTimer != null && runningTimer.timer is PlaybackTimer.Epoch) {
    val timer = runningTimer.timer as PlaybackTimer.Epoch
    val elapsed = kotlin.time.Clock.System.now().toEpochMilliseconds() - runningTimer.startedAt
    val remainingMs = timer.epochMillis - elapsed

    val context = LocalContext.current
    val color = contentColor.getColor(context).toArgb()
    val remoteViews = remember(remainingMs, color) {
      RemoteViews(context.packageName, R.layout.widget_sleep_timer_button).apply {
        setChronometerCountDown(R.id.chronometer, true)
        setChronometer(R.id.chronometer, SystemClock.elapsedRealtime() + remainingMs, null, true)
        setTextColor(R.id.chronometer, color)
        setInt(R.id.icon, "setColorFilter", color)
      }
    }

    Box(
      modifier = modifier
        .clickable(actionRunCallback(ClearSleepTimerActionCallback::class.java))
        .background(containerColor)
        .cornerRadius(20.dp)
        .padding(
          horizontal = 16.dp,
          vertical = 10.dp,
        ),
      contentAlignment = Alignment.Center,
    ) {
      AndroidRemoteViews(
        remoteViews = remoteViews,
      )
    }
  } else if (runningTimer != null && runningTimer.timer is PlaybackTimer.EndOfChapter) {
    OutlineButton(
      text = "Ch.",
      icon = ImageProvider(R.drawable.ic_media_snooze),
      onClick = actionRunCallback(ClearSleepTimerActionCallback::class.java),
      contentColor = inactiveColor,
      modifier = modifier,
    )
  } else {
    OutlineButton(
      text = "${sleepTimerDuration.inWholeMinutes}m",
      icon = ImageProvider(R.drawable.ic_media_snooze),
      onClick = actionRunCallback(SleepTimerActionCallback::class.java),
      contentColor = inactiveColor,
      modifier = modifier,
    )
  }
}
