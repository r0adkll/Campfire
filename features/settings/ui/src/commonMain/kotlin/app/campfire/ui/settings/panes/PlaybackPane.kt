package app.campfire.ui.settings.panes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.SwitchSetting
import app.campfire.ui.settings.composables.TimeJumpSetting
import app.campfire.ui.settings.composables.TimeJumps
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.setting_playback_backward_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_backward_title
import campfire.features.settings.ui.generated.resources.setting_playback_forward_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_forward_title
import campfire.features.settings.ui.generated.resources.setting_playback_mp3seeking_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_mp3seeking_title
import campfire.features.settings.ui.generated.resources.setting_playback_title
import campfire.features.settings.ui.generated.resources.setting_playback_track_reset_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_track_reset_title
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlaybackPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_playback_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    TimeJumpSetting(
      time = state.playbackSettings.forwardTime,
      onTimeChange = {
        state.eventSink(PlaybackSettingEvent.ForwardTime(it))
      },
      jumps = QuickSkipJumps,
      headlineContent = { Text(stringResource(Res.string.setting_playback_forward_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_forward_subtitle)) },
    )

    TimeJumpSetting(
      time = state.playbackSettings.backwardTime,
      onTimeChange = {
        state.eventSink(PlaybackSettingEvent.BackwardTime(it))
      },
      jumps = QuickSkipJumps,
      headlineContent = { Text(stringResource(Res.string.setting_playback_backward_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_backward_subtitle)) },
    )

    TimeJumpSetting(
      time = state.playbackSettings.trackResetThreshold,
      onTimeChange = {
        state.eventSink(PlaybackSettingEvent.TrackResetThreshold(it))
      },
      jumps = QuickSkipJumps,
      headlineContent = { Text(stringResource(Res.string.setting_playback_track_reset_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_track_reset_subtitle)) },
    )

    SwitchSetting(
      value = state.playbackSettings.mp3IndexSeeking,
      onValueChange = {
        state.eventSink(PlaybackSettingEvent.Mp3IndexSeeking(it))
      },
      headlineContent = { Text(stringResource(Res.string.setting_playback_mp3seeking_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_mp3seeking_subtitle)) },
    )
  }
}

enum class QuickSkipJumps(val duration: Duration) {
  Small(5.seconds),
  Medium(10.seconds),
  Large(30.seconds),
  ;

  fun next(): QuickSkipJumps {
    val newOrdinal = (ordinal + 1) % QuickSkipJumps.entries.size
    return QuickSkipJumps.entries[newOrdinal]
  }

  companion object : TimeJumps {
    override fun nextFrom(duration: Duration): Duration {
      val jump = entries.find { it.duration == duration } ?: Medium
      return jump.next().duration
    }
  }
}
