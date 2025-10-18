package app.campfire.audioplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.audioplayer.ui.composables.RunningTimerCard
import app.campfire.audioplayer.ui.composables.SessionSheetLayout
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.extensions.readoutAtMost
import app.campfire.core.isDebug
import campfire.infra.audioplayer.public_ui.generated.resources.Res
import campfire.infra.audioplayer.public_ui.generated.resources.action_set_timer
import campfire.infra.audioplayer.public_ui.generated.resources.timer_bottomsheet_title
import campfire.infra.audioplayer.public_ui.generated.resources.timer_custom
import campfire.infra.audioplayer.public_ui.generated.resources.timer_end_of_chapter
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
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
      Impression {
        ScreenViewEvent("SleepTimer", ScreenType.Overlay)
      }

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

      val timers = remember {
        buildList {
          if (isDebug) add(10.seconds)
          addAll(DefaultTimers.map { it.minutes })
        }
      }

      timers.forEach { timerDuration ->
        ListItem(
          headlineContent = { Text(timerDuration.readoutAtMost(DurationUnit.MINUTES)) },
          modifier = Modifier.clickable {
            onTimerSelected(PlaybackTimer.Epoch(timerDuration.inWholeMilliseconds))
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

private val DefaultTimers = listOf(5, 10, 15, 30, 45, 60, 90)
