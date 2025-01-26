package app.campfire.ui.settings

import androidx.compose.runtime.Immutable
import app.campfire.common.screens.SettingsScreen
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Server
import app.campfire.core.model.Tent
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.SleepSettings.AutoSleepTimer
import app.campfire.settings.api.SleepSettings.ShakeSensitivity
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlin.time.Duration
import kotlinx.datetime.LocalTime

data class SettingsUiState(
  val server: LoadState<out Server>,
  val theme: CampfireSettings.Theme,
  val useDynamicColors: Boolean,
  val applicationInfo: ApplicationInfo,
  val playbackSettings: PlaybackSettingsInfo,
  val sleepSettings: SleepSettingsInfo,
  val eventSink: (SettingsUiEvent) -> Unit,
) : CircuitUiState

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

enum class SettingsPane {
  Account,
  Appearance,
  Playback,
  Sleep,
  About,
  ;

  val screenPage: SettingsScreen.Page get() = when (this) {
    Account -> SettingsScreen.Page.Account
    Appearance -> SettingsScreen.Page.Appearance
    Playback -> SettingsScreen.Page.Playback
    Sleep -> SettingsScreen.Page.Sleep
    About -> SettingsScreen.Page.About
  }
}

sealed interface SettingsUiEvent : CircuitUiEvent {
  data object Back : SettingsUiEvent
  data class SettingsPaneClick(val pane: SettingsPane) : SettingsUiEvent

  // Account Pane Events - TODO: Move to sealed interface
  data class ChangeTent(val tent: Tent) : SettingsUiEvent
  data class ChangeName(val name: String) : SettingsUiEvent
  data object Logout : SettingsUiEvent

  // Appearance Pane Events - TODO: Move to sealed interface
  data class Theme(val theme: CampfireSettings.Theme) : SettingsUiEvent
  data class UseDynamicColors(val useDynamicColors: Boolean) : SettingsUiEvent

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
    data class AutoSleepTimer(val timer: SleepSettings.AutoSleepTimer) : SleepSettingEvent
    data class AutoSleepRewindEnabled(val enabled: Boolean) : SleepSettingEvent
    data class AutoSleepRewindAmount(val amount: Duration) : SleepSettingEvent
  }

  sealed interface AboutSettingEvent : SettingsUiEvent {
    data object DeveloperClick : AboutSettingEvent
    data object GithubClick : AboutSettingEvent
    data object PrivacyPolicyClick : AboutSettingEvent
    data object TermsOfServiceClick : AboutSettingEvent
    data object AttributionsClick : AboutSettingEvent
  }
}
