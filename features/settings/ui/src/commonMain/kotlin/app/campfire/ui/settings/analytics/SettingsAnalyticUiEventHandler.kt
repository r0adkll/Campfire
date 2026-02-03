package app.campfire.ui.settings.analytics

import app.campfire.analytics.Analytics
import app.campfire.analytics.events.Click
import app.campfire.analytics.events.SettingActionEvent
import app.campfire.analytics.events.Updated
import app.campfire.analytics.events.Verb
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.AttributionsClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.ChangelogClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.DeveloperClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.GithubClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.PrivacyPolicyClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.TermsOfServiceClick
import app.campfire.ui.settings.SettingsUiEvent.AccountSettingEvent.ChangeName
import app.campfire.ui.settings.SettingsUiEvent.AccountSettingEvent.ChangeTent
import app.campfire.ui.settings.SettingsUiEvent.AccountSettingEvent.Logout
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.DynamicItemDetailTheming
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.DynamicPlaybackTheming
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.Theme
import app.campfire.ui.settings.SettingsUiEvent.DownloadsSettingEvent.DeleteDownload
import app.campfire.ui.settings.SettingsUiEvent.DownloadsSettingEvent.DownloadClicked
import app.campfire.ui.settings.SettingsUiEvent.DownloadsSettingEvent.ShowDownloadConfirmation
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.BackwardTime
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.ForwardTime
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.Mp3IndexSeeking
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.TrackResetThreshold
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepRewindAmount
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepRewindEnabled
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepTimer
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepTimerEnabled
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepTimerEnd
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepTimerStart
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.ShakeSensitivity
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.ShakeToReset
import me.tatarka.inject.annotations.Inject

@Inject
class SettingsAnalyticUiEventHandler(
  private val analytics: Analytics,
) {

  private fun send(obj: String, verb: Verb, noun: Any? = null) {
    analytics.send(SettingActionEvent(obj, verb, noun))
  }

  fun handle(event: SettingsUiEvent) = when (event) {
    SettingsUiEvent.Back -> Unit
    is SettingsUiEvent.SettingsPaneClick -> Unit

    is SettingsUiEvent.AccountSettingEvent -> when (event) {
      is ChangeName -> send("account_name", Updated)
      is ChangeTent -> send("account_tent", Updated)
      Logout -> send("logout", Click)
    }

    is SettingsUiEvent.AppearanceSettingEvent -> when (event) {
      is Theme -> send("theme", Updated, event.themeMode.storageKey)
      is DynamicItemDetailTheming -> send("dynamic_item_detail_theme", Updated, event.enabled.toString())
      is DynamicPlaybackTheming -> send("dynamic_playback_theme", Updated, event.enabled.toString())
      SettingsUiEvent.AppearanceSettingEvent.OpenThemeBuilder -> send("edit_theme", Click)
    }

    is SettingsUiEvent.DownloadsSettingEvent -> when (event) {
      is ShowDownloadConfirmation -> send("show_confirm_download", Updated, event.enabled.toString())
      is DeleteDownload -> send("delete_download", Click)
      is DownloadClicked -> send("download", Click)
    }

    is SettingsUiEvent.PlaybackSettingEvent -> when (event) {
      is ForwardTime -> send("forward_time", Updated, event.forwardTime.inWholeMilliseconds)
      is BackwardTime -> send("backward_time", Updated, event.backwardTime.inWholeMilliseconds)
      is TrackResetThreshold -> send("track_reset_threshold", Updated, event.trackResetThreshold.inWholeMilliseconds)
      is Mp3IndexSeeking -> send("mp3_index_seeking", Updated, event.mp3IndexSeeking)
    }

    is SettingsUiEvent.SleepSettingEvent -> when (event) {
      is ShakeToReset -> send("shake_to_reset", Updated, event.enabled)
      is ShakeSensitivity -> send("shake_sensitivity", Updated, event.sensitivity.storageKey)
      is AutoSleepTimerEnabled -> send("auto_sleep", Updated, event.enabled)
      is AutoSleepTimerStart -> send("auto_sleep_start", Updated, event.time.toString())
      is AutoSleepTimerEnd -> send("auto_sleep_end", Updated, event.time.toString())
      is AutoSleepTimer -> when (event.timer) {
        is PlaybackTimer.EndOfChapter -> send("sleep_timer", Updated, "end_of_chapter")
        is PlaybackTimer.Epoch -> send("sleep_timer", Updated, "epoc")
      }

      is AutoSleepRewindEnabled -> send("auto_sleep_rewind", Updated, event.enabled)
      is AutoSleepRewindAmount -> send("auto_sleep_rewind_amount", Updated, event.amount.inWholeMilliseconds)
    }

    is SettingsUiEvent.AboutSettingEvent -> when (event) {
      ChangelogClick -> send("changelog", Click)
      AttributionsClick -> send("attributions", Click)
      DeveloperClick -> send("developer", Click)
      GithubClick -> send("contribute", Click)
      PrivacyPolicyClick -> send("privacy_policy", Click)
      TermsOfServiceClick -> send("terms_of_service", Click)
      is SettingsUiEvent.AboutSettingEvent.AnalyticReportingEnabled -> Unit
      is SettingsUiEvent.AboutSettingEvent.CrashReportingEnabled -> {
        send("crash_reporting", Updated, event.enabled)
      }
    }

    is SettingsUiEvent.DeveloperSettingEvent -> Unit
  }
}
