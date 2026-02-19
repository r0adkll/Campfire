package app.campfire.sessions.ui.sheets.sleeptimer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
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
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.audioplayer.ui.composables.RunningTimerCard
import app.campfire.audioplayer.ui.composables.SessionSheetLayout
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.extensions.clockFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.BookRibbon
import app.campfire.common.compose.icons.rounded.BookRibbon
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.seconds
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.core.model.preview.libraryItem
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.SleepSettings
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_clear_timer
import campfire.features.sessions.ui.generated.resources.action_set_timer
import campfire.features.sessions.ui.generated.resources.option_shake_to_reset_subtitle
import campfire.features.sessions.ui.generated.resources.option_shake_to_reset_title
import campfire.features.sessions.ui.generated.resources.timer_bottomsheet_title
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

internal sealed interface TimerResult {
  data object None : TimerResult
  data object Cleared : TimerResult
  data class Selected(
    val timer: PlaybackTimer,
  ) : TimerResult
}

internal sealed interface TimerModel {
  data object None : TimerModel
  data class Running(val timer: RunningTimer) : TimerModel
}

@ContributesTo(UserScope::class)
interface SleepTimerBottomSheetComponent {
  val sessionsRepository: SessionsRepository
  val sleepSettings: SleepSettings
}

internal suspend fun OverlayHost.showSleepTimerBottomSheet(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerBottomSheetV2(
  runningTimer: RunningTimer?,
  modifier: Modifier = Modifier,
  onTimerSelected: (PlaybackTimer) -> Unit,
  onTimerCleared: () -> Unit,
  component: SleepTimerBottomSheetComponent = rememberComponent(),
) {
  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.timer_bottomsheet_title)) },
  ) {
    AnimatedContent(
      targetState = runningTimer,
    ) { timer ->
      if (timer != null) {
        ActiveTimerSheetContent(
          runningTimer = timer,
          onTimerCleared = onTimerCleared,
        )
      } else {
        InactiveTimerSheetContent(
          component = component,
          onTimerSelected = onTimerSelected,
        )
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

@Composable
private fun ActiveTimerSheetContent(
  runningTimer: RunningTimer,
  onTimerCleared: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
  ) {
    RunningTimerCard(
      runningTimer = runningTimer,
    )

    ClearTimerButton(
      onClick = onTimerCleared,
      modifier = Modifier
        .padding(
          horizontal = 16.dp,
        ),
    )
  }
}

@Composable
private fun InactiveTimerSheetContent(
  component: SleepTimerBottomSheetComponent,
  onTimerSelected: (PlaybackTimer) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    var isEpochTimeSelection by remember { mutableStateOf(true) }

    TimerTypeSelector(
      isEpochTimeSelection = isEpochTimeSelection,
      onTimerTypeChange = { isEpochTimeSelection = it },
      modifier = Modifier.padding(horizontal = 24.dp),
    )

    Spacer(Modifier.height(24.dp))

    val timerInputState = rememberTimePickerState(
      initialHour = component.sleepSettings.lastSetSleepTimer.inWholeHours.toInt(),
      initialMinute = (component.sleepSettings.lastSetSleepTimer.inWholeMinutes % 60).toInt(),
      is24Hour = true,
    )

    LaunchedEffect(timerInputState.hour, timerInputState.minute) {
      component.sleepSettings.lastSetSleepTimer = timerInputState.hour.hours + timerInputState.minute.minutes
    }

    val shakeToReset by remember {
      component.sleepSettings.observeShakeToResetEnabled()
    }.collectAsState()

    AnimatedContent(
      targetState = isEpochTimeSelection,
      transitionSpec = {
        if (targetState) {
          slideIntoContainer(SlideDirection.End) togetherWith
            slideOutOfContainer(SlideDirection.End)
        } else {
          slideIntoContainer(SlideDirection.Start) togetherWith
            slideOutOfContainer(SlideDirection.Start)
        }
      },
    ) { isTimeSelection ->
      if (isTimeSelection) {
        EpochTimerContent(
          initialTime = component.sleepSettings.lastSetSleepTimer,
          timeInputState = timerInputState,
          shakeToReset = shakeToReset,
          onShakeToResetChange = {
            component.sleepSettings.shakeToResetEnabled = it
          },
        )
      } else {
        val session by remember {
          component.sessionsRepository.observeCurrentSession()
        }.collectAsState(null)

        EndOfChapterContent(
          session = session,
        )
      }
    }

    Spacer(Modifier.height(16.dp))

    val isEnabled by remember {
      derivedStateOf {
        !isEpochTimeSelection ||
          (timerInputState.hour.hours + timerInputState.minute.minutes)
            .inWholeMilliseconds > 0L
      }
    }

    SetTimerButton(
      enabled = isEnabled,
      onClick = {
        if (isEpochTimeSelection) {
          val customTimer = timerInputState.hour.hours + timerInputState.minute.minutes
          onTimerSelected(PlaybackTimer.Epoch(customTimer.inWholeMilliseconds))
        } else {
          onTimerSelected(PlaybackTimer.EndOfChapter())
        }
      },
      modifier = Modifier
        .padding(
          horizontal = 16.dp,
        ),
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimerTypeSelector(
  isEpochTimeSelection: Boolean,
  onTimerTypeChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    val size = ButtonDefaults.MinHeight
    val colors = ToggleButtonDefaults.tonalToggleButtonColors()
    ToggleButton(
      checked = isEpochTimeSelection,
      onCheckedChange = onTimerTypeChange,
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
        text = "Time",
        style = ButtonDefaults.textStyleFor(size),
      )
    }

    ToggleButton(
      checked = !isEpochTimeSelection,
      onCheckedChange = { onTimerTypeChange(!it) },
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
}

@Composable
private fun EpochTimerContent(
  initialTime: Duration,
  timeInputState: TimePickerState,
  shakeToReset: Boolean,
  onShakeToResetChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  timers: List<Int> = DefaultTimers,
) {
  Column(modifier = modifier) {
    var timeInputCanFocus by remember { mutableStateOf(false) }
    TimeInput(
      state = timeInputState,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .focusProperties {
          canFocus = timeInputCanFocus
        },
    )

    // This is a hack to prevent the keyboard from showing by default
    LaunchedEffect(Unit) {
      delay(300L)
      timeInputCanFocus = true
    }

    var sliderValue by remember {
      val startIndex = timers.indexOfFirst { it.minutes > initialTime }
        .let { if (it > 0) it - 1 else 0 }
        .toFloat()
      mutableFloatStateOf(startIndex)
    }
    Slider(
      value = sliderValue,
      steps = timers.size - 2,
      onValueChange = { value ->
        sliderValue = value
        val index = value.roundToInt()
        val time = timers[index].minutes
        timeInputState.hourInput = time.inWholeHours.toInt()
        timeInputState.minuteInput = (time.inWholeMinutes % 60).toInt()
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
          checked = shakeToReset,
          onCheckedChange = onShakeToResetChange,
        )
      },
      colors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
      ),
      modifier = Modifier
        .clickable {
          onShakeToResetChange(!shakeToReset)
        },
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EndOfChapterContent(
  session: Session?,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        vertical = 32.dp,
        horizontal = 32.dp,
      ),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val remainingTimeInChapter = session?.chapter?.let { chapter ->
      (chapter.end.seconds - session.currentTime)
        .clockFormat()
    } ?: "--"

    Text(
      text = "Time remaining",
      style = MaterialTheme.typography.labelMediumEmphasized,
    )

    Text(
      text = remainingTimeInChapter,
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.displayMedium,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier,
    )

    session?.chapter?.let { chapter ->
      Spacer(Modifier.height(4.dp))
      Text(
        text = chapter.title,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .fillMaxWidth(),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetTimerButton(
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val buttonSize = ButtonDefaults.MediumContainerHeight
  val contentPadding = ButtonDefaults.contentPaddingFor(buttonSize)
  Button(
    enabled = enabled,
    shapes = ButtonDefaults.shapes(
      pressedShape = ButtonDefaults.mediumPressedShape,
    ),
    onClick = onClick,
    contentPadding = contentPadding,
    modifier = modifier
      .heightIn(buttonSize)
      .fillMaxWidth(),
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClearTimerButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val buttonSize = ButtonDefaults.MediumContainerHeight
  val contentPadding = ButtonDefaults.contentPaddingFor(buttonSize)
  Button(
    shapes = ButtonDefaults.shapes(
      pressedShape = ButtonDefaults.mediumPressedShape,
    ),
    onClick = onClick,
    contentPadding = contentPadding,
    modifier = modifier
      .heightIn(buttonSize)
      .fillMaxWidth(),
  ) {
    Icon(
      Icons.Rounded.TimerOff,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonSize)),
    )
    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonSize)))
    Text(
      text = stringResource(Res.string.action_clear_timer),
      style = ButtonDefaults.textStyleFor(buttonSize),
    )
  }
}

private val DefaultTimers = listOf(0, 1, 5, 10, 15, 30, 45, 60, 90, 120, 150, 180, 210, 240)

@Preview
@Composable
fun TimerBottomSheetV2Preview() {
  CampfireTheme {
    ModalBottomSheet(
      sheetState = rememberModalBottomSheetState(true),
      onDismissRequest = {},
    ) {
      var timer by remember {
        mutableStateOf<RunningTimer?>(
          RunningTimer(
            timer = PlaybackTimer.Epoch(15_000L),
            startedAt = Clock.System.now().toEpochMilliseconds() - 7500L,
            isShakeToRestartEnabled = true,
          ),
        )
      }

      TimerBottomSheetV2(
        runningTimer = timer,
        onTimerSelected = {
          timer = RunningTimer(
            timer = it,
            startedAt = Clock.System.now().toEpochMilliseconds(),
            isShakeToRestartEnabled = false,
          )
        },
        onTimerCleared = {
          timer = null
        },
        component = object : SleepTimerBottomSheetComponent {
          override val sleepSettings: SleepSettings = object : SleepSettings {
            override var lastSetSleepTimer: Duration = 5.minutes

            val mutableShakeToReset = MutableStateFlow(true)
            override var shakeToResetEnabled: Boolean
              get() = mutableShakeToReset.value
              set(value) { mutableShakeToReset.value = value }

            override fun observeShakeToResetEnabled(): StateFlow<Boolean> {
              return mutableShakeToReset
            }

            override var shakeSensitivity: SleepSettings.ShakeSensitivity
              get() = TODO("Not yet implemented")
              set(value) {}

            override fun observeShakeSensitivity(): StateFlow<SleepSettings.ShakeSensitivity> {
              TODO("Not yet implemented")
            }

            override var autoSleepTimerEnabled: Boolean
              get() = TODO("Not yet implemented")
              set(value) {}

            override fun observeAutoSleepTimerEnabled(): StateFlow<Boolean> {
              TODO("Not yet implemented")
            }

            override var autoSleepStart: LocalTime
              get() = TODO("Not yet implemented")
              set(value) {}

            override fun observeAutoSleepStart(): StateFlow<LocalTime> {
              TODO("Not yet implemented")
            }

            override var autoSleepEnd: LocalTime
              get() = TODO("Not yet implemented")
              set(value) {}

            override fun observeAutoSleepEnd(): StateFlow<LocalTime> {
              TODO("Not yet implemented")
            }

            override var autoSleepTimer: SleepSettings.AutoSleepTimer
              get() = TODO("Not yet implemented")
              set(value) {}

            override fun observeAutoSleepTimer(): StateFlow<SleepSettings.AutoSleepTimer> {
              TODO("Not yet implemented")
            }

            override var autoRewindEnabled: Boolean
              get() = TODO("Not yet implemented")
              set(value) {}

            override fun observeAutoRewindEnabled(): StateFlow<Boolean> {
              TODO("Not yet implemented")
            }

            override var autoRewindAmount: Duration
              get() = TODO("Not yet implemented")
              set(value) {}

            override fun observeAutoRewindAmount(): StateFlow<Duration> {
              TODO("Not yet implemented")
            }
          }
          override val sessionsRepository: SessionsRepository
            get() = object : SessionsRepository {
              override suspend fun getSession(libraryItemId: LibraryItemId): Session? = null
              override suspend fun getCurrentSession(): Session? = null
              override suspend fun createSession(libraryItemId: LibraryItemId): Session = error("n/a")
              override suspend fun markDeleted(libraryItemId: LibraryItemId) = Unit
              override suspend fun deleteSession(libraryItemId: LibraryItemId) = Unit
              override suspend fun updateCurrentTime(
                libraryItemId: LibraryItemId,
                currentTime: Duration,
              ) = Unit
              override suspend fun addTimeListening(
                libraryItemId: LibraryItemId,
                amount: Duration,
              ) = Unit
              override suspend fun stopSession(libraryItemId: LibraryItemId) = Unit
              override suspend fun markFinished(libraryItemId: LibraryItemId) = Unit
              override fun observeCurrentSession(): Flow<Session?> {
                return flowOf(
                  Session(
                    id = Uuid.random(),
                    libraryItem = libraryItem(
                      id = "item_id",
                      duration = 23.hours,
                    ),
                    userId = "user_id",
                    isDeleted = false,
                    playMethod = PlayMethod.DirectStream,
                    mediaPlayer = "media_player",
                    timeListening = 0.seconds,
                    startTime = 0.seconds,
                    currentTime = 5.hours,
                    startedAt = LocalDateTime(2026, 1, 1, 1, 1),
                    updatedAt = LocalDateTime(2026, 1, 1, 1, 1),
                  ),
                )
              }
            }
        },
      )
    }
  }
}
