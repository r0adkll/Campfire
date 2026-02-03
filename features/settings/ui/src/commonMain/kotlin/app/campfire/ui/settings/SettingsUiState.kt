package app.campfire.ui.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Server
import app.campfire.core.model.Tent
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.SleepSettings.AutoSleepTimer
import app.campfire.settings.api.SleepSettings.ShakeSensitivity
import app.campfire.settings.api.ThemeMode
import app.campfire.ui.theming.api.AppTheme
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlin.time.Duration
import kotlinx.datetime.LocalTime

@Stable
data class SettingsUiState(
  val server: LoadState<out Server>,
  val isShakingAvailable: Boolean,
  val applicationInfo: ApplicationInfo,
  val appearanceSettings: AppearanceSettingsInfo,
  val downloadsSettings: DownloadsSettingsInfo,
  val playbackSettings: PlaybackSettingsInfo,
  val sleepSettings: SleepSettingsInfo,
  val aboutSettings: AboutSettingsInfo,
  val developerSettings: DeveloperSettingsInfo,
  val eventSink: (SettingsUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class AppearanceSettingsInfo(
  val appTheme: AppTheme,
  val themeMode: ThemeMode,
  val dynamicItemDetailTheming: Boolean,
  val dynamicPlaybackTheming: Boolean,
)

@Immutable
data class DownloadsSettingsInfo(
  val showDownloadConfirmation: Boolean,
  val downloads: Map<LibraryItem, OfflineDownload>,
)

@Immutable
data class PlaybackSettingsInfo(
  val forwardTime: Duration,
  val backwardTime: Duration,
  val trackResetThreshold: Duration,
  val mp3IndexSeeking: Boolean,
)

@Immutable
data class SleepSettingsInfo(
  val shakeToReset: Boolean,
  val shakeSensitivity: ShakeSensitivity,
  val autoSleepSetting: AutoSleepSetting? = null,
) {

  @Immutable
  data class AutoSleepSetting(
    val start: LocalTime,
    val end: LocalTime,
    val timer: AutoSleepTimer,
    val rewindEnabled: Boolean,
    val rewindAmount: Duration,
  )
}

@Immutable
data class AboutSettingsInfo(
  val crashReportingEnabled: Boolean,
  val analyticReportingEnabled: Boolean,
)

@Immutable
data class DeveloperSettingsInfo(
  val developerModeEnabled: Boolean,
  val sessionAge: Duration,
  val showWidgetPinningPrompt: Boolean,
  val analyticsDebugState: String,
  val isAndroidAutoAvailable: Boolean,
)

enum class SettingsPane {
  Account,
  Appearance,
  Downloads,
  Playback,
  Sleep,
  About,
  Developer,
  ;

  val screenPage: SettingsScreen.Page get() = when (this) {
    Account -> SettingsScreen.Page.Account
    Appearance -> SettingsScreen.Page.Appearance
    Downloads -> SettingsScreen.Page.Downloads
    Playback -> SettingsScreen.Page.Playback
    Sleep -> SettingsScreen.Page.Sleep
    About -> SettingsScreen.Page.About
    Developer -> SettingsScreen.Page.Developer
  }
}

sealed interface SettingsUiEvent : CircuitUiEvent {
  data object Back : SettingsUiEvent
  data class SettingsPaneClick(val pane: SettingsPane) : SettingsUiEvent

  // Account Pane Events
  sealed interface AccountSettingEvent : SettingsUiEvent {
    data class ChangeTent(val tent: Tent) : AccountSettingEvent
    data class ChangeName(val name: String) : AccountSettingEvent
    data object Logout : AccountSettingEvent
  }

  // Appearance Pane Events
  sealed interface AppearanceSettingEvent : SettingsUiEvent {
    data class Theme(val themeMode: ThemeMode) : AppearanceSettingEvent
    data class DynamicItemDetailTheming(val enabled: Boolean) : AppearanceSettingEvent
    data class DynamicPlaybackTheming(val enabled: Boolean) : AppearanceSettingEvent
    data object OpenThemeBuilder : AppearanceSettingEvent
  }

  // Downloads Pane Events
  sealed interface DownloadsSettingEvent : SettingsUiEvent {
    data class ShowDownloadConfirmation(val enabled: Boolean) : DownloadsSettingEvent
    data class DownloadClicked(val libraryItem: LibraryItem) : DownloadsSettingEvent
    data class DeleteDownload(val libraryItem: LibraryItem) : DownloadsSettingEvent
  }

  // Playback Setting Events
  sealed interface PlaybackSettingEvent : SettingsUiEvent {
    data class ForwardTime(val forwardTime: Duration) : PlaybackSettingEvent
    data class BackwardTime(val backwardTime: Duration) : PlaybackSettingEvent
    data class TrackResetThreshold(val trackResetThreshold: Duration) : PlaybackSettingEvent
    data class Mp3IndexSeeking(val mp3IndexSeeking: Boolean) : PlaybackSettingEvent
  }

  // Sleep Setting Events
  sealed interface SleepSettingEvent : SettingsUiEvent {
    data class ShakeToReset(val enabled: Boolean) : SleepSettingEvent
    data class ShakeSensitivity(val sensitivity: SleepSettings.ShakeSensitivity) : SleepSettingEvent
    data class AutoSleepTimerEnabled(val enabled: Boolean) : SleepSettingEvent
    data class AutoSleepTimerStart(val time: LocalTime) : SleepSettingEvent
    data class AutoSleepTimerEnd(val time: LocalTime) : SleepSettingEvent
    data class AutoSleepTimer(val timer: PlaybackTimer) : SleepSettingEvent
    data class AutoSleepRewindEnabled(val enabled: Boolean) : SleepSettingEvent
    data class AutoSleepRewindAmount(val amount: Duration) : SleepSettingEvent
  }

  sealed interface AboutSettingEvent : SettingsUiEvent {
    data object DeveloperClick : AboutSettingEvent
    data object GithubClick : AboutSettingEvent
    data object PrivacyPolicyClick : AboutSettingEvent
    data object TermsOfServiceClick : AboutSettingEvent
    data object AttributionsClick : AboutSettingEvent
    data object ChangelogClick : AboutSettingEvent
    data class CrashReportingEnabled(val enabled: Boolean) : AboutSettingEvent
    data class AnalyticReportingEnabled(val enabled: Boolean) : AboutSettingEvent
  }

  sealed interface DeveloperSettingEvent : SettingsUiEvent {
    data object EnableDeveloperMode : DeveloperSettingEvent
    data object OpenAndroidAutoSettings : DeveloperSettingEvent
    data object InvalidateCurrentAccount : DeveloperSettingEvent
    data class SessionAge(val sessionAge: Duration) : DeveloperSettingEvent
    data class ShowWidgetPinningChange(val enabled: Boolean) : DeveloperSettingEvent
  }
}
