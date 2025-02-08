package app.campfire.ui.settings.bottomsheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.clockFormat
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.action_clear_timer
import campfire.features.settings.ui.generated.resources.action_set_timer
import campfire.features.settings.ui.generated.resources.label_current_timer
import campfire.features.settings.ui.generated.resources.timer_bottomsheet_title
import campfire.features.settings.ui.generated.resources.timer_custom
import campfire.features.settings.ui.generated.resources.timer_end_of_chapter
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource

sealed interface TimerResult {
  data object None : TimerResult
  data object Cleared : TimerResult
  data class Selected(val timer: PlaybackTimer) : TimerResult
}

sealed interface TimerModel {
  data object None : TimerModel
  data class Running(val timer: RunningTimer) : TimerModel
}

// TODO: Commonize this component. This is duplicated from the session module.
suspend fun OverlayHost.showTimerBottomSheet(
  runningTimer: RunningTimer? = null,
): TimerResult {
  return show(
    BottomSheetOverlay<TimerModel, TimerResult>(
      model = runningTimer?.let { TimerModel.Running(it) } ?: TimerModel.None,
      onDismiss = { TimerResult.None },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      skipPartiallyExpandedState = true,
    ) { model, overlayNavigator ->
      TimerBottomSheet(
        runningTimer = (model as? TimerModel.Running)?.timer,
        onTimerSelected = { timer ->
          overlayNavigator.finish(TimerResult.Selected(timer))
        },
        onTimerCleared = {
          overlayNavigator.finish(TimerResult.Cleared)
        },
      )
    },
  )
}

@Composable
private fun SessionSheetLayout(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier,
  ) {
    Box(
      Modifier
        .padding(16.dp)
        .align(Alignment.CenterHorizontally),
    ) {
      ProvideTextStyle(
        MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold,
        ),
      ) {
        title()
      }
    }

    content()
  }
}

@Composable
private fun TimerBottomSheet(
  runningTimer: RunningTimer?,
  modifier: Modifier = Modifier,
  onTimerSelected: (PlaybackTimer) -> Unit,
  onTimerCleared: () -> Unit,
) {
  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.timer_bottomsheet_title)) },
  ) {
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
      if (runningTimer != null) {
        RunningTimerCard(
          runningTimer = runningTimer,
          onTimerCleared = onTimerCleared,
        )
      }

      DefaultTimers.forEach { timerInMin ->
        ListItem(
          headlineContent = { Text("$timerInMin min") },
          modifier = Modifier.clickable {
            onTimerSelected(PlaybackTimer.Epoch(timerInMin.minutes.inWholeMilliseconds))
          },
          colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
          ),
        )
      }

      ListItem(
        headlineContent = { Text(stringResource(Res.string.timer_end_of_chapter)) },
        modifier = Modifier.clickable {
          onTimerSelected(PlaybackTimer.EndOfChapter())
        },
        colors = ListItemDefaults.colors(
          containerColor = Color.Transparent,
        ),
      )

      var isVisible by remember { mutableStateOf(false) }
      ListItem(
        headlineContent = { Text(stringResource(Res.string.timer_custom)) },
        colors = ListItemDefaults.colors(
          containerColor = Color.Transparent,
        ),
        modifier = Modifier.clickable {
          isVisible = !isVisible
        },
      )

      AnimatedVisibility(isVisible) {
        Column {
          val timerInputState = rememberTimePickerState(is24Hour = true)
          TimeInput(
            state = timerInputState,
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .focusable(),
          )

          val isEnabled by remember {
            derivedStateOf {
              (timerInputState.hour.hours + timerInputState.minute.minutes)
                .inWholeMilliseconds > 0L
            }
          }

          Button(
            enabled = isEnabled,
            onClick = {
              val customTimer = timerInputState.hour.hours + timerInputState.minute.minutes
              onTimerSelected(PlaybackTimer.Epoch(customTimer.inWholeMilliseconds))
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp),
          ) {
            Text(stringResource(Res.string.action_set_timer))
          }
        }
      }

      Spacer(Modifier.height(16.dp))
      Spacer(
        Modifier
          .navigationBarsPadding()
          .imePadding(),
      )
    }
  }
}

@Composable
private fun RunningTimerCard(
  runningTimer: RunningTimer,
  onTimerCleared: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 16.dp, horizontal = 16.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    tonalElevation = 4.dp,
    shadowElevation = 1.dp,
  ) {
    Column {
      // Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp),
      ) {
        Icon(
          Icons.Outlined.Timer,
          contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(Res.string.label_current_timer),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
      }

      RunningTimerText(
        runningTimer = runningTimer,
        style = { timer ->
          when (timer) {
            is PlaybackTimer.EndOfChapter -> MaterialTheme.typography.titleLarge
            is PlaybackTimer.Epoch -> MaterialTheme.typography.displayMedium
          }
        },
        modifier = Modifier.align(Alignment.CenterHorizontally),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .height(56.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onTimerCleared,
        ) {
          Text(stringResource(Res.string.action_clear_timer))
        }
      }
    }
  }
}

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

private val DefaultTimers = listOf(5, 10, 15, 30, 45, 60, 90)
