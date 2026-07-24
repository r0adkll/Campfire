package app.campfire.ui.settings.panes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.settings.api.MinPauseThresholdRange
import app.campfire.settings.api.ResumeRewindRange
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.DurationRangeSliderSetting
import app.campfire.ui.settings.composables.DurationSliderSetting
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.ResumeRewindPreviewRow
import app.campfire.ui.settings.composables.SwitchSetting
import app.campfire.ui.settings.composables.TimeJumpSetting
import app.campfire.ui.settings.composables.TimeJumps
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.header_auto_rewind_on_resume
import campfire.features.settings.ui.generated.resources.header_playback_history
import campfire.features.settings.ui.generated.resources.header_player_interface
import campfire.features.settings.ui.generated.resources.header_synchronization
import campfire.features.settings.ui.generated.resources.setting_playback_auto_rewind_on_resume_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_auto_rewind_on_resume_title
import campfire.features.settings.ui.generated.resources.setting_playback_auto_sync_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_auto_sync_title
import campfire.features.settings.ui.generated.resources.setting_playback_backward_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_backward_title
import campfire.features.settings.ui.generated.resources.setting_playback_book_time_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_book_time_title
import campfire.features.settings.ui.generated.resources.setting_playback_forward_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_forward_title
import campfire.features.settings.ui.generated.resources.setting_playback_history_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_history_title
import campfire.features.settings.ui.generated.resources.setting_playback_min_pause_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_min_pause_title
import campfire.features.settings.ui.generated.resources.setting_playback_mp3seeking_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_mp3seeking_title
import campfire.features.settings.ui.generated.resources.setting_playback_remote_skip_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_remote_skip_title
import campfire.features.settings.ui.generated.resources.setting_playback_resume_rewind_preview_header
import campfire.features.settings.ui.generated.resources.setting_playback_rewind_range_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_rewind_range_title
import campfire.features.settings.ui.generated.resources.setting_playback_rewind_stop_chapter_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_rewind_stop_chapter_title
import campfire.features.settings.ui.generated.resources.setting_playback_sync_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_sync_title
import campfire.features.settings.ui.generated.resources.setting_playback_title
import campfire.features.settings.ui.generated.resources.setting_playback_track_reset_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_track_reset_title
import campfire.features.settings.ui.generated.resources.setting_playback_wavy_scrubber_subtitle
import campfire.features.settings.ui.generated.resources.setting_playback_wavy_scrubber_title
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

    SwitchSetting(
      value = state.playbackSettings.remoteNextPrevSkipsChapters,
      onValueChange = {
        state.eventSink(PlaybackSettingEvent.RemoteNextPrevSkipsChapters(it))
      },
      headlineContent = { Text(stringResource(Res.string.setting_playback_remote_skip_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_remote_skip_subtitle)) },
    )

    Header(
      title = { Text(stringResource(Res.string.header_player_interface)) },
    )

    SwitchSetting(
      value = state.playbackSettings.bookTimeInPlaybackUi,
      onValueChange = {
        state.eventSink(PlaybackSettingEvent.BookTimeInPlaybackUi(it))
      },
      headlineContent = { Text(stringResource(Res.string.setting_playback_book_time_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_book_time_subtitle)) },
    )

    SwitchSetting(
      value = state.playbackSettings.playbackWavyScrubber,
      onValueChange = {
        state.eventSink(PlaybackSettingEvent.PlaybackWavyScrubber(it))
      },
      headlineContent = { Text(stringResource(Res.string.setting_playback_wavy_scrubber_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_wavy_scrubber_subtitle)) },
    )

    Header(
      title = { Text(stringResource(Res.string.header_auto_rewind_on_resume)) },
    )

    SwitchSetting(
      value = state.playbackSettings.autoRewindOnResumeEnabled,
      onValueChange = {
        state.eventSink(PlaybackSettingEvent.AutoRewindOnResumeEnabled(it))
      },
      headlineContent = { Text(stringResource(Res.string.setting_playback_auto_rewind_on_resume_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_auto_rewind_on_resume_subtitle)) },
    )

    AnimatedVisibility(
      visible = state.playbackSettings.autoRewindOnResumeEnabled,
    ) {
      Column {
        val config = state.playbackSettings.resumeRewindConfig

        DurationSliderSetting(
          title = stringResource(Res.string.setting_playback_min_pause_title),
          subtitle = stringResource(Res.string.setting_playback_min_pause_subtitle),
          value = config.minPauseThreshold,
          valueRange = MinPauseThresholdRange,
          stepSeconds = 1,
          onValueChange = { state.eventSink(PlaybackSettingEvent.MinPauseThreshold(it)) },
        )

        DurationRangeSliderSetting(
          title = stringResource(Res.string.setting_playback_rewind_range_title),
          subtitle = stringResource(Res.string.setting_playback_rewind_range_subtitle),
          min = config.minRewind,
          max = config.maxRewind,
          valueRange = ResumeRewindRange,
          stepSeconds = 5,
          onValueChange = { min, max ->
            state.eventSink(PlaybackSettingEvent.ResumeRewindRange(min, max))
          },
        )

        SwitchSetting(
          value = state.playbackSettings.autoRewindStopAtChapterBoundary,
          onValueChange = {
            state.eventSink(PlaybackSettingEvent.AutoRewindStopAtChapterBoundary(it))
          },
          headlineContent = { Text(stringResource(Res.string.setting_playback_rewind_stop_chapter_title)) },
          supportingContent = { Text(stringResource(Res.string.setting_playback_rewind_stop_chapter_subtitle)) },
        )

        Header(
          title = { Text(stringResource(Res.string.setting_playback_resume_rewind_preview_header)) },
        )

        state.playbackSettings.resumeRewindPreview.forEach { tier ->
          ResumeRewindPreviewRow(tier = tier)
        }
      }
    }

    Header(
      title = { Text(stringResource(Res.string.header_synchronization)) },
    )

    SwitchSetting(
      value = state.playbackSettings.syncEnabled,
      onValueChange = {
        state.eventSink(PlaybackSettingEvent.SyncEnabled(it))
      },
      headlineContent = { Text(stringResource(Res.string.setting_playback_sync_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_sync_subtitle)) },
    )

    AnimatedVisibility(
      visible = state.playbackSettings.syncEnabled,
    ) {
      SwitchSetting(
        value = state.playbackSettings.autoSyncEnabled,
        onValueChange = {
          state.eventSink(PlaybackSettingEvent.AutoSyncEnabled(it))
        },
        headlineContent = { Text(stringResource(Res.string.setting_playback_auto_sync_title)) },
        supportingContent = { Text(stringResource(Res.string.setting_playback_auto_sync_subtitle)) },
      )
    }

    Header(
      title = { Text(stringResource(Res.string.header_playback_history)) },
    )

    SwitchSetting(
      value = state.playbackSettings.playbackHistoryEnabled,
      onValueChange = {
        state.eventSink(PlaybackSettingEvent.PlaybackHistoryEnabled(it))
      },
      headlineContent = { Text(stringResource(Res.string.setting_playback_history_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_playback_history_subtitle)) },
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
