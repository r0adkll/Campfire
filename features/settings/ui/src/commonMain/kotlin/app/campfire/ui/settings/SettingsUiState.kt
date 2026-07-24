package app.campfire.ui.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.Server
import app.campfire.core.model.Tent
import app.campfire.settings.api.AndroidAutoCategory
import app.campfire.settings.api.AndroidAutoCategoryConfig
import app.campfire.settings.api.ResumeRewindConfig
import app.campfire.settings.api.ResumeRewindTier
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
  val isAndroidAutoPaneVisible: Boolean,
  val applicationInfo: ApplicationInfo,
  val socketSyncEnabled: Boolean,
  val appearanceSettings: AppearanceSettingsInfo,
  val downloadsSettings: DownloadsSettingsInfo,
  val playbackSettings: PlaybackSettingsInfo,
  val sleepSettings: SleepSettingsInfo,
  val androidAutoSettings: AndroidAutoSettingsInfo,
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
  val itemCardMarqueeEnabled: Boolean,
)

@Immutable
data class DownloadsSettingsInfo(
  val showDownloadConfirmation: Boolean,
  val downloads: List<DownloadEntry>,
)

@Immutable
sealed interface DownloadEntry {
  val libraryItem: LibraryItem
  val download: OfflineDownload

  data class Book(
    override val libraryItem: LibraryItem,
    override val download: OfflineDownload,
  ) : DownloadEntry

  data class Episode(
    override val libraryItem: LibraryItem,
    val episode: PodcastEpisode,
    override val download: OfflineDownload,
  ) : DownloadEntry
}

@Immutable
data class PlaybackSettingsInfo(
  val forwardTime: Duration,
  val backwardTime: Duration,
  val trackResetThreshold: Duration,
  val mp3IndexSeeking: Boolean,
  val remoteNextPrevSkipsChapters: Boolean,
  val syncEnabled: Boolean,
  val autoSyncEnabled: Boolean,
  val playbackHistoryEnabled: Boolean,
  val autoRewindOnResumeEnabled: Boolean,
  val resumeRewindConfig: ResumeRewindConfig,
  val resumeRewindPreview: List<ResumeRewindTier>,
  val autoRewindStopAtChapterBoundary: Boolean,
  val bookTimeInPlaybackUi: Boolean,
  val playbackWavyScrubber: Boolean,
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
  val showAppUpdateSignIn: Boolean,
)

@Immutable
data class AndroidAutoSettingsInfo(
  val isAndroidAutoAvailable: Boolean,
  val categories: List<AndroidAutoCategoryConfig>,
)

@Immutable
data class DeveloperSettingsInfo(
  val developerModeEnabled: Boolean,
  val sessionAge: Duration,
  val showWidgetPinningPrompt: Boolean,
  val analyticsDebugState: String,
  val mediaButtonPackages: Set<String>,
  val fakeAppUpdateSignedIn: Boolean,
  val fakeAppUpdateAvailable: Boolean,
  val fakeAppUpdateFailDownload: Boolean,
)

enum class SettingsPane {
  Account,
  Appearance,
  Downloads,
  Playback,
  Sleep,
  AndroidAuto,
  About,
  Developer,
  ;

  val screenPage: SettingsScreen.Page get() = when (this) {
    Account -> SettingsScreen.Page.Account
    Appearance -> SettingsScreen.Page.Appearance
    Downloads -> SettingsScreen.Page.Downloads
    Playback -> SettingsScreen.Page.Playback
    Sleep -> SettingsScreen.Page.Sleep
    AndroidAuto -> SettingsScreen.Page.AndroidAuto
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
    data class SocketSyncEnabled(val enabled: Boolean) : AccountSettingEvent
    data object Logout : AccountSettingEvent
  }

  // Appearance Pane Events
  sealed interface AppearanceSettingEvent : SettingsUiEvent {
    data class Theme(val themeMode: ThemeMode) : AppearanceSettingEvent
    data class DynamicItemDetailTheming(val enabled: Boolean) : AppearanceSettingEvent
    data class DynamicPlaybackTheming(val enabled: Boolean) : AppearanceSettingEvent
    data class ItemCardMarqueeEnabled(val enabled: Boolean) : AppearanceSettingEvent
    data object OpenThemeBuilder : AppearanceSettingEvent
  }

  // Downloads Pane Events
  sealed interface DownloadsSettingEvent : SettingsUiEvent {
    data class ShowDownloadConfirmation(val enabled: Boolean) : DownloadsSettingEvent
    data class DownloadClicked(val entry: DownloadEntry) : DownloadsSettingEvent
    data class DeleteDownload(val entry: DownloadEntry) : DownloadsSettingEvent
  }

  // Playback Setting Events
  sealed interface PlaybackSettingEvent : SettingsUiEvent {
    data class ForwardTime(val forwardTime: Duration) : PlaybackSettingEvent
    data class BackwardTime(val backwardTime: Duration) : PlaybackSettingEvent
    data class TrackResetThreshold(val trackResetThreshold: Duration) : PlaybackSettingEvent
    data class Mp3IndexSeeking(val mp3IndexSeeking: Boolean) : PlaybackSettingEvent
    data class RemoteNextPrevSkipsChapters(val remoteNextPrevSkipsChapters: Boolean) : PlaybackSettingEvent
    data class SyncEnabled(val enabled: Boolean) : PlaybackSettingEvent
    data class AutoSyncEnabled(val enabled: Boolean) : PlaybackSettingEvent
    data class PlaybackHistoryEnabled(val enabled: Boolean) : PlaybackSettingEvent
    data class AutoRewindOnResumeEnabled(val enabled: Boolean) : PlaybackSettingEvent
    data class MinPauseThreshold(val threshold: Duration) : PlaybackSettingEvent
    data class ResumeRewindRange(
      val minRewind: Duration,
      val maxRewind: Duration,
    ) : PlaybackSettingEvent
    data class AutoRewindStopAtChapterBoundary(val enabled: Boolean) : PlaybackSettingEvent
    data class BookTimeInPlaybackUi(val enabled: Boolean) : PlaybackSettingEvent
    data class PlaybackWavyScrubber(val enabled: Boolean) : PlaybackSettingEvent
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
    data object AppUpdateSignInClick : AboutSettingEvent
  }

  sealed interface DeveloperSettingEvent : SettingsUiEvent {
    data object EnableDeveloperMode : DeveloperSettingEvent
    data object InvalidateCurrentAccount : DeveloperSettingEvent
    data object ClearMediaButtonPackages : DeveloperSettingEvent
    data class SessionAge(val sessionAge: Duration) : DeveloperSettingEvent
    data class ShowWidgetPinningChange(val enabled: Boolean) : DeveloperSettingEvent
    data class FakeAppUpdateSignedIn(val enabled: Boolean) : DeveloperSettingEvent
    data class FakeAppUpdateAvailable(val enabled: Boolean) : DeveloperSettingEvent
    data class FakeAppUpdateFailDownload(val enabled: Boolean) : DeveloperSettingEvent
    data object ResetAppUpdateDismissals : DeveloperSettingEvent
  }

  // Android Auto Pane Events
  sealed interface AndroidAutoSettingEvent : SettingsUiEvent {
    data object OpenAndroidAutoSettings : AndroidAutoSettingEvent
    data class SetCategoryVisible(
      val category: AndroidAutoCategory,
      val visible: Boolean,
    ) : AndroidAutoSettingEvent
    data class SetCategoryGridLayout(
      val category: AndroidAutoCategory,
      val isGrid: Boolean,
    ) : AndroidAutoSettingEvent
    data class ReorderCategories(val order: List<AndroidAutoCategory>) : AndroidAutoSettingEvent
  }
}
