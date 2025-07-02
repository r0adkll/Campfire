package app.campfire.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.common.screens.AttributionScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.ApplicationUrls
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.DevSettings
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.SleepSettings
import app.campfire.shake.ShakeDetector
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.AttributionsClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.DeveloperClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.GithubClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.PrivacyPolicyClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.TermsOfServiceClick
import app.campfire.ui.settings.SettingsUiEvent.AccountSettingEvent.ChangeName
import app.campfire.ui.settings.SettingsUiEvent.AccountSettingEvent.ChangeTent
import app.campfire.ui.settings.SettingsUiEvent.AccountSettingEvent.Logout
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.Theme
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.UseDynamicColors
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
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(SettingsScreen::class, UserScope::class)
@Inject
class SettingsPresenter(
  @Assisted private val navigator: Navigator,
  private val applicationInfo: ApplicationInfo,
  private val applicationUrls: ApplicationUrls,
  private val settings: CampfireSettings,
  private val playbackSettings: PlaybackSettings,
  private val sleepSettings: SleepSettings,
  private val devSettings: DevSettings,
  private val serverRepository: ServerRepository,
  private val accountManager: AccountManager,
  private val shakeDetector: ShakeDetector,
) : Presenter<SettingsUiState> {

  @Composable
  override fun present(): SettingsUiState {
    val scope = rememberCoroutineScope()

    val server by remember {
      serverRepository.observeCurrentServer()
        .map { LoadState.Loaded(it) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    // Appearance Settings
    val theme by remember { settings.observeTheme() }.collectAsState(settings.theme)
    val useDynamicColors by remember { settings.observeUseDynamicColors() }.collectAsState(settings.useDynamicColors)

    // Playback Settings
    val forwardTime by remember { playbackSettings.observeForwardTimeMs() }.collectAsState()
    val backwardTime by remember { playbackSettings.observeBackwardTimeMs() }.collectAsState()
    val trackResetThreshold by remember { playbackSettings.observeTrackResetThreshold() }.collectAsState()
    val mp3IndexSeeking by remember { playbackSettings.observeMp3IndexSeeking() }.collectAsState()

    // Downloads Settings
    val showDownloadConfirmation by remember { settings.observeShowConfirmDownload() }
      .collectAsState(settings.showConfirmDownload)

    // Sleep Settings
    val shakeToResetEnabled by remember { sleepSettings.observeShakeToResetEnabled() }.collectAsState()
    val shakeSensitivity by remember { sleepSettings.observeShakeSensitivity() }.collectAsState()
    val autoSleepTimerEnabled by remember { sleepSettings.observeAutoSleepTimerEnabled() }.collectAsState()
    val autoSleepTimerStart by remember { sleepSettings.observeAutoSleepStart() }.collectAsState()
    val autoSleepTimerEnd by remember { sleepSettings.observeAutoSleepEnd() }.collectAsState()
    val autoSleepTimer by remember { sleepSettings.observeAutoSleepTimer() }.collectAsState()
    val autoSleepRewindEnabled by remember { sleepSettings.observeAutoRewindEnabled() }.collectAsState()
    val autoSleepRewindAmount by remember { sleepSettings.observeAutoRewindAmount() }.collectAsState()

    // Developer Settings
    val sessionAge by remember { devSettings.observeSessionAge() }.collectAsState()
    val showWidgetPinningPrompt by remember { settings.observeHasShownWidgetPinning() }.collectAsState(false)

    return SettingsUiState(
      server = server,
      theme = theme,
      isShakingAvailable = remember { shakeDetector.isAvailable },
      useDynamicColors = useDynamicColors,
      applicationInfo = applicationInfo,
      downloadsSettings = DownloadsSettingsInfo(
        showDownloadConfirmation = showDownloadConfirmation,
      ),
      playbackSettings = PlaybackSettingsInfo(
        forwardTime = forwardTime.milliseconds,
        backwardTime = backwardTime.milliseconds,
        trackResetThreshold = trackResetThreshold,
        mp3IndexSeeking = mp3IndexSeeking,
      ),
      sleepSettings = SleepSettingsInfo(
        shakeToReset = shakeToResetEnabled,
        shakeSensitivity = shakeSensitivity,
        autoSleepSetting = if (autoSleepTimerEnabled) {
          SleepSettingsInfo.AutoSleepSetting(
            start = autoSleepTimerStart,
            end = autoSleepTimerEnd,
            timer = autoSleepTimer,
            rewindEnabled = autoSleepRewindEnabled,
            rewindAmount = autoSleepRewindAmount,
          )
        } else {
          null
        },
      ),
      developerSettings = DeveloperSettingsInfo(
        sessionAge = sessionAge,
        showWidgetPinningPrompt = showWidgetPinningPrompt,
      ),
    ) { event ->
      when (event) {
        SettingsUiEvent.Back -> navigator.pop()
        is SettingsUiEvent.SettingsPaneClick -> navigator.goTo(SettingsScreen(event.pane.screenPage))

        is SettingsUiEvent.AccountSettingEvent -> when (event) {
          is ChangeName -> {
            scope.launch { serverRepository.changeName(event.name) }
          }

          is ChangeTent -> {
            scope.launch { serverRepository.changeTent(event.tent) }
          }

          Logout -> {
            scope.launch { accountManager.logout(server.dataOrNull!!) }
          }
        }

        is SettingsUiEvent.AppearanceSettingEvent -> when (event) {
          is Theme -> settings.theme = event.theme
          is UseDynamicColors -> settings.useDynamicColors = event.useDynamicColors
        }

        is SettingsUiEvent.DownloadsSettingEvent -> when (event) {
          is ShowDownloadConfirmation -> settings.showConfirmDownload = event.enabled
        }

        is SettingsUiEvent.PlaybackSettingEvent -> when (event) {
          is ForwardTime -> playbackSettings.forwardTimeMs = event.forwardTime.inWholeMilliseconds
          is BackwardTime -> playbackSettings.backwardTimeMs = event.backwardTime.inWholeMilliseconds
          is TrackResetThreshold -> playbackSettings.trackResetThreshold = event.trackResetThreshold
          is Mp3IndexSeeking -> playbackSettings.enableMp3IndexSeeking = event.mp3IndexSeeking
        }

        is SettingsUiEvent.SleepSettingEvent -> when (event) {
          is ShakeToReset -> sleepSettings.shakeToResetEnabled = event.enabled
          is ShakeSensitivity -> sleepSettings.shakeSensitivity = event.sensitivity
          is AutoSleepTimerEnabled -> sleepSettings.autoSleepTimerEnabled = event.enabled
          is AutoSleepTimerStart -> sleepSettings.autoSleepStart = event.time
          is AutoSleepTimerEnd -> sleepSettings.autoSleepEnd = event.time
          is AutoSleepTimer -> sleepSettings.autoSleepTimer = when (val timer = event.timer) {
            is PlaybackTimer.EndOfChapter -> SleepSettings.AutoSleepTimer.EndOfChapter
            is PlaybackTimer.Epoch -> SleepSettings.AutoSleepTimer.Epoch(timer.epochMillis)
          }

          is AutoSleepRewindEnabled -> sleepSettings.autoRewindEnabled = event.enabled
          is AutoSleepRewindAmount -> sleepSettings.autoRewindAmount = event.amount
        }

        is SettingsUiEvent.AboutSettingEvent -> when (event) {
          AttributionsClick -> navigator.goTo(AttributionScreen)
          DeveloperClick -> navigator.goTo(UrlScreen(applicationUrls.developerHomepage))
          GithubClick -> navigator.goTo(UrlScreen(applicationUrls.githubDiscussion))
          PrivacyPolicyClick -> navigator.goTo(UrlScreen(applicationUrls.privacyPolicy))
          TermsOfServiceClick -> navigator.goTo(UrlScreen(applicationUrls.termsOfService))
        }

        is SettingsUiEvent.DeveloperSettingEvent -> when (event) {
          is SettingsUiEvent.DeveloperSettingEvent.SessionAge -> devSettings.sessionAge = event.sessionAge
          is SettingsUiEvent.DeveloperSettingEvent.ShowWidgetPinningChange ->
            settings.hasShownWidgetPinning = event.enabled
        }
      }
    }
  }
}
