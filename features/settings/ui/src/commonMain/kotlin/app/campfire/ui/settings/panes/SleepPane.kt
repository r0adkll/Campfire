package app.campfire.ui.settings.panes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import app.campfire.audioplayer.ui.TimerResult
import app.campfire.audioplayer.ui.showTimerBottomSheet
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ShakeHigh
import app.campfire.common.compose.icons.rounded.ShakeLow
import app.campfire.common.compose.icons.rounded.ShakeMedium
import app.campfire.common.compose.icons.rounded.ShakeVeryHigh
import app.campfire.common.compose.icons.rounded.ShakeVeryLow
import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.core.extensions.capitalized
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.SleepSettings.ShakeSensitivity
import app.campfire.settings.api.SleepSettings.ShakeSensitivity.High
import app.campfire.settings.api.SleepSettings.ShakeSensitivity.Low
import app.campfire.settings.api.SleepSettings.ShakeSensitivity.Medium
import app.campfire.settings.api.SleepSettings.ShakeSensitivity.VeryHigh
import app.campfire.settings.api.SleepSettings.ShakeSensitivity.VeryLow
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.ShakeToReset
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.DropdownSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.LocalTimeSetting
import app.campfire.ui.settings.composables.SwitchSetting
import app.campfire.ui.settings.composables.TimeJumpSetting
import app.campfire.ui.settings.composables.TimeJumps
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.header_auto_sleep
import campfire.features.settings.ui.generated.resources.header_shake_to_reset
import campfire.features.settings.ui.generated.resources.setting_auto_sleep_auto_rewind_amount_title
import campfire.features.settings.ui.generated.resources.setting_auto_sleep_auto_rewind_subtitle
import campfire.features.settings.ui.generated.resources.setting_auto_sleep_auto_rewind_title
import campfire.features.settings.ui.generated.resources.setting_auto_sleep_end_title
import campfire.features.settings.ui.generated.resources.setting_auto_sleep_start_title
import campfire.features.settings.ui.generated.resources.setting_auto_sleep_subtitle
import campfire.features.settings.ui.generated.resources.setting_auto_sleep_timer
import campfire.features.settings.ui.generated.resources.setting_auto_sleep_title
import campfire.features.settings.ui.generated.resources.setting_playback_shake_sensitivity_title
import campfire.features.settings.ui.generated.resources.setting_playback_sleep_shake_to_reset_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_sleep_shake_to_reset_title
import campfire.features.settings.ui.generated.resources.setting_sleep_title
import campfire.features.settings.ui.generated.resources.shake_sensitivity_high
import campfire.features.settings.ui.generated.resources.shake_sensitivity_low
import campfire.features.settings.ui.generated.resources.shake_sensitivity_medium
import campfire.features.settings.ui.generated.resources.shake_sensitivity_very_high
import campfire.features.settings.ui.generated.resources.shake_sensitivity_very_low
import campfire.features.settings.ui.generated.resources.timer_end_of_chapter
import com.slack.circuit.overlay.LocalOverlayHost
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SleepPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_sleep_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    if (currentPlatform != Platform.DESKTOP && state.isShakingAvailable) {
      Header(
        title = { Text(stringResource(Res.string.header_shake_to_reset)) },
      )

      // Shake to Reset
      SwitchSetting(
        value = state.sleepSettings.shakeToReset,
        onValueChange = { state.eventSink(ShakeToReset(it)) },
        headlineContent = { Text(stringResource(Res.string.setting_playback_sleep_shake_to_reset_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_playback_sleep_shake_to_reset_subtitle)) },
      )

      // Shake Sensitivity
      AnimatedVisibility(
        visible = state.sleepSettings.shakeToReset,
      ) {
        DropdownSetting(
          value = state.sleepSettings.shakeSensitivity,
          values = ShakeSensitivity.entries,
          onValueChange = { sensitivity ->
            state.eventSink(SleepSettingEvent.ShakeSensitivity(sensitivity))
          },
          headlineContent = { Text(stringResource(Res.string.setting_playback_shake_sensitivity_title)) },
          itemIcon = { sensitivity ->
            Icon(
              sensitivity.asImageVector,
              contentDescription = sensitivity.name.capitalized(),
            )
          },
          itemText = { sensitivity ->
            Text(stringResource(sensitivity.asStringResource))
          },
        )
      }
    }

    Header(
      title = { Text(stringResource(Res.string.header_auto_sleep)) },
    )

    // Auto Sleep
    SwitchSetting(
      value = state.sleepSettings.autoSleepSetting != null,
      onValueChange = { state.eventSink(SleepSettingEvent.AutoSleepTimerEnabled(it)) },
      headlineContent = { Text(stringResource(Res.string.setting_auto_sleep_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_auto_sleep_subtitle)) },
    )

    AnimatedVisibility(
      visible = state.sleepSettings.autoSleepSetting != null,
    ) {
      Column {
        LocalTimeSetting(
          value = state.sleepSettings.autoSleepSetting?.start ?: LocalTime(0, 0),
          onValueChange = { state.eventSink(SleepSettingEvent.AutoSleepTimerStart(it)) },
          headlineContent = { Text(stringResource(Res.string.setting_auto_sleep_start_title)) },
        )

        LocalTimeSetting(
          value = state.sleepSettings.autoSleepSetting?.end ?: LocalTime(0, 0),
          onValueChange = { state.eventSink(SleepSettingEvent.AutoSleepTimerEnd(it)) },
          headlineContent = { Text(stringResource(Res.string.setting_auto_sleep_end_title)) },
        )

        val scope = rememberCoroutineScope()
        val overlayHost = LocalOverlayHost.current
        ActionSetting(
          headlineContent = { Text(stringResource(Res.string.setting_auto_sleep_timer)) },
          trailingContent = {
            Text(
              text = when (val timer = state.sleepSettings.autoSleepSetting?.timer) {
                SleepSettings.AutoSleepTimer.EndOfChapter -> stringResource(Res.string.timer_end_of_chapter)
                is SleepSettings.AutoSleepTimer.Epoch -> timer.millis.milliseconds.thresholdReadoutFormat(
                  thresholds = mapOf(DurationUnit.MINUTES to 120), // 2hrs
                )
                null -> ""
              },
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
          },
          onClick = {
            scope.launch {
              when (val result = overlayHost.showTimerBottomSheet()) {
                is TimerResult.Selected -> state.eventSink(SleepSettingEvent.AutoSleepTimer(result.timer))
                else -> Unit
              }
            }
          },
        )

        SwitchSetting(
          value = state.sleepSettings.autoSleepSetting?.rewindEnabled == true,
          onValueChange = { state.eventSink(SleepSettingEvent.AutoSleepRewindEnabled(it)) },
          headlineContent = { Text(stringResource(Res.string.setting_auto_sleep_auto_rewind_title)) },
          supportingContent = { Text(stringResource(Res.string.setting_auto_sleep_auto_rewind_subtitle)) },
        )

        AnimatedVisibility(
          visible = state.sleepSettings.autoSleepSetting?.rewindEnabled == true,
        ) {
          TimeJumpSetting(
            time = state.sleepSettings.autoSleepSetting?.rewindAmount ?: AutoRewindJumps.Default,
            onTimeChange = { state.eventSink(SleepSettingEvent.AutoSleepRewindAmount(it)) },
            jumps = AutoRewindJumps,
            headlineContent = { Text(stringResource(Res.string.setting_auto_sleep_auto_rewind_amount_title)) },
          )
        }
      }
    }
  }
}

val ShakeSensitivity.asImageVector: ImageVector get() = when (this) {
  VeryLow -> CampfireIcons.Rounded.ShakeVeryLow
  Low -> CampfireIcons.Rounded.ShakeLow
  Medium -> CampfireIcons.Rounded.ShakeMedium
  High -> CampfireIcons.Rounded.ShakeHigh
  VeryHigh -> CampfireIcons.Rounded.ShakeVeryHigh
}

val ShakeSensitivity.asStringResource: StringResource get() = when (this) {
  VeryLow -> Res.string.shake_sensitivity_very_low
  Low -> Res.string.shake_sensitivity_low
  Medium -> Res.string.shake_sensitivity_medium
  High -> Res.string.shake_sensitivity_high
  VeryHigh -> Res.string.shake_sensitivity_very_high
}

enum class AutoRewindJumps(val duration: Duration) {
  Short(1.minutes),
  Medium(5.minutes),
  Long(10.minutes),
  VeryLong(15.minutes),
  ;

  fun next(): AutoRewindJumps {
    val newOrdinal = (ordinal + 1) % AutoRewindJumps.entries.size
    return AutoRewindJumps.entries[newOrdinal]
  }

  companion object : TimeJumps {
    val Default: Duration get() = Medium.duration

    override fun nextFrom(duration: Duration): Duration {
      val jump = AutoRewindJumps.entries.find { it.duration == duration } ?: Medium
      return jump.next().duration
    }
  }
}
