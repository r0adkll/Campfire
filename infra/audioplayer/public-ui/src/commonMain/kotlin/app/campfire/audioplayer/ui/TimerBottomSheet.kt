package app.campfire.audioplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.audioplayer.ui.composables.RunningTimerCard
import app.campfire.audioplayer.ui.composables.SessionSheetLayout
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.extensions.readoutAtMost
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.BookRibbon
import app.campfire.common.compose.icons.rounded.BookRibbon
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.extensions.fluentIf
import app.campfire.core.isDebug
import campfire.infra.audioplayer.public_ui.generated.resources.Res
import campfire.infra.audioplayer.public_ui.generated.resources.action_set_timer
import campfire.infra.audioplayer.public_ui.generated.resources.option_shake_to_reset_subtitle
import campfire.infra.audioplayer.public_ui.generated.resources.option_shake_to_reset_title
import campfire.infra.audioplayer.public_ui.generated.resources.timer_bottomsheet_title
import campfire.infra.audioplayer.public_ui.generated.resources.timer_custom
import campfire.infra.audioplayer.public_ui.generated.resources.timer_end_of_chapter
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

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

      TimerBottomSheetV2(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerBottomSheetV2(
  runningTimer: RunningTimer?,
  modifier: Modifier = Modifier,
  onTimerSelected: (PlaybackTimer) -> Unit,
  onTimerCleared: () -> Unit,
  timers: List<Int> = DefaultTimers2,
) {
  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.timer_bottomsheet_title)) },
  ) {
    var isEpochTimeSelection by remember { mutableStateOf(true) }
    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
      val size = ButtonDefaults.ExtraSmallContainerHeight
      val colors = ToggleButtonDefaults.tonalToggleButtonColors()
      ToggleButton(
        checked = isEpochTimeSelection,
        onCheckedChange = { isEpochTimeSelection = it },
        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(size),
        colors = colors,
        modifier = Modifier
          .heightIn(size)
          .weight(1f)
          .semantics { role = Role.RadioButton },
      ) {
        Icon(
          if (isEpochTimeSelection) {
            Icons.Rounded.Timer
          } else {
            Icons.Outlined.Timer
          },
          contentDescription = "Timer by time",
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
        Text(
          text = "By time",
          style = ButtonDefaults.textStyleFor(size),
        )
      }

      ToggleButton(
        checked = !isEpochTimeSelection,
        onCheckedChange = { isEpochTimeSelection = !it },
        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(size),
        colors = colors,
        modifier = Modifier
          .heightIn(size)
          .weight(1f)
          .semantics { role = Role.RadioButton },
      ) {
        Icon(
          if (!isEpochTimeSelection) {
            CampfireIcons.Rounded.BookRibbon
          } else {
            CampfireIcons.Filled.BookRibbon
          },
          contentDescription = "Timer by chapter",
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
        Text(
          text = "End of Chapter",
          style = ButtonDefaults.textStyleFor(size),
        )
      }
    }

    Spacer(Modifier.height(16.dp))

    val timerInputState = rememberTimePickerState(is24Hour = true)
    var shakeToReset by remember { mutableStateOf(false) }

    var timeInputCanFocus by remember { mutableStateOf(false) }
    TimeInput(
      state = timerInputState,
      colors = TimePickerDefaults.colors()
        .fluentIf(!isEpochTimeSelection) {
          val containerColor = MaterialTheme.colorScheme.scrim.copy(0.25f)
          val contentColor = Color.White.copy(0.5f)
          copy(
            periodSelectorBorderColor = containerColor,
            timeSelectorSelectedContainerColor = containerColor,
            timeSelectorSelectedContentColor = contentColor,
            timeSelectorUnselectedContainerColor = containerColor,
            timeSelectorUnselectedContentColor = contentColor,
          )
        },
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .focusProperties {
          canFocus = timeInputCanFocus
        },
    )

    LaunchedEffect(isEpochTimeSelection) {
      delay(300L)
      timeInputCanFocus = isEpochTimeSelection
    }

    var sliderValue by remember { mutableFloatStateOf(0f) }
    Slider(
      value = sliderValue,
      steps = timers.size,
      enabled = isEpochTimeSelection,
      onValueChange = { value ->
        sliderValue = value
        val index = value.toInt()
        val time = timers[index].minutes
        timerInputState.hourInput = time.inWholeHours.toInt()
        timerInputState.minuteInput = (time.inWholeMinutes % 60).toInt()
      },
      valueRange = 0f..(timers.size.toFloat() - 1f),
      modifier = Modifier
        .padding(
          horizontal = 20.dp,
        ),
    )

    Spacer(Modifier.height(16.dp))

    ListItem(
      headlineContent = { Text(stringResource(Res.string.option_shake_to_reset_title)) },
      supportingContent = { Text(stringResource(Res.string.option_shake_to_reset_subtitle)) },
      trailingContent = {
        Switch(
          enabled = isEpochTimeSelection,
          checked = shakeToReset,
          onCheckedChange = { shakeToReset = it },
        )
      },
      colors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
      ),
      modifier = modifier
        .clickable(enabled = isEpochTimeSelection) {
          shakeToReset = !shakeToReset
        },
    )

    Spacer(Modifier.height(16.dp))

    val isEnabled by remember {
      derivedStateOf {
        !isEpochTimeSelection ||
          (timerInputState.hour.hours + timerInputState.minute.minutes)
            .inWholeMilliseconds > 0L
      }
    }

    val buttonSize = ButtonDefaults.MediumContainerHeight
    val contentPadding = ButtonDefaults.contentPaddingFor(buttonSize)
    Button(
      enabled = isEnabled,
      shapes = ButtonDefaults.shapes(
        pressedShape = ButtonDefaults.mediumPressedShape,
      ),
      onClick = {
        if (isEpochTimeSelection) {
          val customTimer = timerInputState.hour.hours + timerInputState.minute.minutes
          onTimerSelected(PlaybackTimer.Epoch(customTimer.inWholeMilliseconds))
        } else {
          onTimerSelected(PlaybackTimer.EndOfChapter())
        }
      },
      contentPadding = contentPadding,
      modifier = Modifier
        .heightIn(buttonSize)
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
    ) {
      Icon(
        Icons.Rounded.Timer,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonSize)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonSize)))
      Text(
        text = stringResource(Res.string.action_set_timer),
        style = ButtonDefaults.textStyleFor(buttonSize),
      )
    }

    Spacer(Modifier.height(16.dp))
    Spacer(
      Modifier
        .navigationBarsPadding()
        .imePadding(),
    )
  }
}

private val DefaultTimers = listOf(5, 10, 15, 30, 45, 60, 90)
private val DefaultTimers2 = listOf(0, 1, 5, 10, 15, 30, 45, 60, 90, 120, 150, 180, 210, 240)

@Preview
@Composable
fun TimerBottomSheetPreview() {
  CampfireTheme {
    ModalBottomSheet(
      sheetState = rememberModalBottomSheetState(true),
      onDismissRequest = {},
    ) {
      TimerBottomSheet(
        runningTimer = RunningTimer(
          timer = PlaybackTimer.Epoch(15_000L),
          startedAt = Clock.System.now().toEpochMilliseconds() - 7500L,
          isShakeToRestartEnabled = true,
        ),
        onTimerSelected = {},
        onTimerCleared = {},
      )
    }
  }
}

@Preview
@Composable
fun TimerBottomSheetV2Preview() {
  CampfireTheme {
    ModalBottomSheet(
      sheetState = rememberModalBottomSheetState(true),
      onDismissRequest = {},
    ) {
      TimerBottomSheetV2(
        runningTimer = RunningTimer(
          timer = PlaybackTimer.Epoch(15_000L),
          startedAt = Clock.System.now().toEpochMilliseconds() - 7500L,
          isShakeToRestartEnabled = true,
        ),
        onTimerSelected = {},
        onTimerCleared = {},
      )
    }
  }
}
