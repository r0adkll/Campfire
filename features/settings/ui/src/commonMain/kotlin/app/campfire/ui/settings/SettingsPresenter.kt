// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.analytics.Analytics
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.AttributionScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.Platform
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.ApplicationUrls
import app.campfire.core.coroutines.LoadState
import app.campfire.core.currentPlatform
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.Media
import app.campfire.core.model.Server
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUser
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.settings.api.AndroidAutoSettings
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.DevSettings
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.SleepSettings
import app.campfire.settings.api.ThemeSettings
import app.campfire.settings.api.tiers
import app.campfire.shake.ShakeDetector
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.AttributionsClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.ChangelogClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.DeveloperClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.GithubClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.PrivacyPolicyClick
import app.campfire.ui.settings.SettingsUiEvent.AboutSettingEvent.TermsOfServiceClick
import app.campfire.ui.settings.SettingsUiEvent.AccountSettingEvent.ChangeName
import app.campfire.ui.settings.SettingsUiEvent.AccountSettingEvent.Logout
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.DynamicItemDetailTheming
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.DynamicPlaybackTheming
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.ItemCardMarqueeEnabled
import app.campfire.ui.settings.SettingsUiEvent.AppearanceSettingEvent.Theme
import app.campfire.ui.settings.SettingsUiEvent.DownloadsSettingEvent.DeleteDownload
import app.campfire.ui.settings.SettingsUiEvent.DownloadsSettingEvent.DownloadClicked
import app.campfire.ui.settings.SettingsUiEvent.DownloadsSettingEvent.ShowDownloadConfirmation
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.AutoRewindOnResumeEnabled
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.AutoRewindStopAtChapterBoundary
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.AutoSyncEnabled
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.BackwardTime
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.BookTimeInPlaybackUi
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.ForwardTime
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.MinPauseThreshold
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.Mp3IndexSeeking
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.PlaybackHistoryEnabled
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.PlaybackWavyScrubber
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.RemoteNextPrevSkipsChapters
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.ResumeRewindRange
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.ServerSessionsEnabled
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.SyncEnabled
import app.campfire.ui.settings.SettingsUiEvent.PlaybackSettingEvent.TrackResetThreshold
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepRewindAmount
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepRewindEnabled
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepTimer
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepTimerEnabled
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepTimerEnd
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.AutoSleepTimerStart
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.ShakeSensitivity
import app.campfire.ui.settings.SettingsUiEvent.SleepSettingEvent.ShakeToReset
import app.campfire.ui.settings.analytics.SettingsAnalyticUiEventHandler
import app.campfire.ui.settings.auto.AndroidAuto
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.updates.source.AppUpdateSource
import app.campfire.whatsnew.api.screen.ChangelogScreen
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@CircuitInject(SettingsScreen::class, UserScope::class)
@Inject
class SettingsPresenter(
  @Assisted private val navigator: Navigator,
  private val userSession: UserSession,
  private val analytics: Analytics,
  private val analyticUiEventHandler: SettingsAnalyticUiEventHandler,
  private val applicationInfo: ApplicationInfo,
  private val applicationUrls: ApplicationUrls,
  private val settings: CampfireSettings,
  private val themeSettings: ThemeSettings,
  private val themeRepository: AppThemeRepository,
  private val playbackSettings: PlaybackSettings,
  private val sleepSettings: SleepSettings,
  private val androidAutoSettings: AndroidAutoSettings,
  private val devSettings: DevSettings,
  private val serverRepository: ServerRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val libraryItemRepository: LibraryItemRepository,
  private val accountManager: AccountManager,
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  private val shakeDetector: ShakeDetector,
  private val androidAuto: AndroidAuto,
  private val appUpdateSource: AppUpdateSource,
) : NonPausablePresenter<SettingsUiState> {

  @Composable
  override fun present(): SettingsUiState {
    val scope = rememberCoroutineScope()

    val server by remember {
      @Suppress("UNCHECKED_CAST")
      serverRepository.observeCurrentServer()
        .map { LoadState.Loaded(it) as LoadState<Server> }
        .catch { emit(LoadState.Error as LoadState<Server>) }
    }.collectAsState(LoadState.Loading)

    // Appearance Settings
    val appTheme by remember { themeRepository.observeCurrentAppTheme() }.collectAsState()
    val themeMode by remember { settings.observeTheme() }.collectAsState()
    val dynamicItemDetailTheming by remember { themeSettings.observeDynamicallyThemeItemDetail() }.collectAsState()
    val dynamicPlaybackTheming by remember { themeSettings.observeDynamicallyThemePlayback() }.collectAsState()
    val itemCardMarqueeEnabled by remember {
      settings.observeLibraryItemMarqueeEnabled()
    }.collectAsState()

    // Playback Settings
    val forwardTime by remember { playbackSettings.observeForwardTimeMs() }.collectAsState()
    val backwardTime by remember { playbackSettings.observeBackwardTimeMs() }.collectAsState()
    val trackResetThreshold by remember { playbackSettings.observeTrackResetThreshold() }.collectAsState()
    val mp3IndexSeeking by remember { playbackSettings.observeMp3IndexSeeking() }.collectAsState()
    val remoteNextPrevSkipsChapters by remember {
      playbackSettings.observeRemoteNextPrevSkipsChapters()
    }.collectAsState()
    val syncEnabled by remember { playbackSettings.observeSyncEnabled() }.collectAsState()
    val autoSyncEnabled by remember { playbackSettings.observeAutoSyncEnabled() }.collectAsState()
    val serverSessionsEnabled by remember { playbackSettings.observeServerSessionsEnabled() }.collectAsState()
    val playbackHistoryEnabled by remember { playbackSettings.observePlaybackHistoryEnabled() }.collectAsState()
    val autoRewindOnResumeEnabled by remember { playbackSettings.observeAutoRewindOnResumeEnabled() }.collectAsState()
    val resumeRewindConfig by remember { playbackSettings.observeResumeRewindConfig() }.collectAsState()
    val resumeRewindPreview = remember(resumeRewindConfig) { resumeRewindConfig.tiers() }
    val autoRewindStopAtChapterBoundary by remember {
      playbackSettings.observeAutoRewindStopAtChapterBoundary()
    }.collectAsState()
    val bookTimeInPlaybackUi by remember { playbackSettings.observeBookTimeInPlaybackUi() }.collectAsState()
    val playbackWavyScrubber by remember { playbackSettings.observePlaybackWavyScrubber() }.collectAsState()

    // Downloads Settings
    val showDownloadConfirmation by remember { settings.observeShowConfirmDownload() }
      .collectAsState()

    val downloads by remember {
      offlineDownloadManager.observeAll()
    }.collectAsState(emptyList())

    val downloadEntries by remember {
      snapshotFlow { downloads }
        .mapLatest { items ->
          items
            .mapNotNull { download ->
              // A download whose item can't be resolved (server unreachable with no cached copy,
              // or deleted on the server) shouldn't take the whole settings screen down.
              val libraryItem = try {
                libraryItemRepository.getLibraryItem(download.libraryItemId)
              } catch (e: CancellationException) {
                throw e
              } catch (e: Exception) {
                bark(throwable = e) { "Unable to resolve library item for download ${download.libraryItemId}" }
                return@mapNotNull null
              }
              val episodeId = download.episodeId
              if (episodeId == null) {
                DownloadEntry.Book(libraryItem, download)
              } else {
                val media = libraryItem.media as? Media.Podcast ?: return@mapNotNull null
                val episode = media.episodes.find { it.id == episodeId }
                  ?: return@mapNotNull null
                DownloadEntry.Episode(libraryItem, episode, download)
              }
            }
            .sortedByDescending { it.download.updateTimeMs }
        }
    }.collectAsState(emptyList())

    // Sleep Settings
    val shakeToResetEnabled by remember { sleepSettings.observeShakeToResetEnabled() }.collectAsState()
    val shakeSensitivity by remember { sleepSettings.observeShakeSensitivity() }.collectAsState()
    val autoSleepTimerEnabled by remember { sleepSettings.observeAutoSleepTimerEnabled() }.collectAsState()
    val autoSleepTimerStart by remember { sleepSettings.observeAutoSleepStart() }.collectAsState()
    val autoSleepTimerEnd by remember { sleepSettings.observeAutoSleepEnd() }.collectAsState()
    val autoSleepTimer by remember { sleepSettings.observeAutoSleepTimer() }.collectAsState()
    val autoSleepRewindEnabled by remember { sleepSettings.observeAutoRewindEnabled() }.collectAsState()
    val autoSleepRewindAmount by remember { sleepSettings.observeAutoRewindAmount() }.collectAsState()

    // About Settings
    val crashReportingEnabled by remember { settings.observeCrashReportingEnabled() }
      .collectAsState()
    val analyticReportingEnabled by remember { settings.observeAnalyticReportingEnabled() }
      .collectAsState()
    val socketSyncEnabled by remember { settings.observeSocketEnabled() }
      .collectAsState()
    val appUpdateSignInDismissed by remember { settings.observeAppUpdateSignInDismissed() }
      .collectAsState()
    var appUpdateInvalidator by remember { mutableIntStateOf(0) }
    val appUpdateSignedIn = remember(appUpdateInvalidator) { appUpdateSource.isSignedIn() }

    // Android Auto Settings
    val androidAutoCategories by remember {
      androidAutoSettings.observeCategoryConfigs()
    }.collectAsState()
    val isAndroidAutoAvailable = remember { androidAuto.isAvailable() }

    // Developer Settings
    val developerModeEnabled by remember { devSettings.observeDeveloperMode() }.collectAsState()
    val sessionAge by remember { devSettings.observeSessionAge() }.collectAsState()
    val showWidgetPinningPrompt by remember { settings.observeHasShownWidgetPinning() }.collectAsState()
    val mediaButtonPackages by remember { devSettings.observeMediaButtonPackages() }.collectAsState()
    val fakeAppUpdateSignedIn by remember { devSettings.observeFakeAppUpdateSignedIn() }.collectAsState()
    val fakeAppUpdateAvailable by remember { devSettings.observeFakeAppUpdateAvailable() }.collectAsState()
    val fakeAppUpdateFailDownload by remember { devSettings.observeFakeAppUpdateFailDownload() }.collectAsState()

    return SettingsUiState(
      server = server,
      isShakingAvailable = remember { shakeDetector.isAvailable },
      isAndroidAutoPaneVisible = currentPlatform == Platform.ANDROID,
      applicationInfo = applicationInfo,
      appearanceSettings = AppearanceSettingsInfo(
        appTheme = appTheme,
        themeMode = themeMode,
        dynamicItemDetailTheming = dynamicItemDetailTheming,
        dynamicPlaybackTheming = dynamicPlaybackTheming,
        itemCardMarqueeEnabled = itemCardMarqueeEnabled,
      ),
      downloadsSettings = DownloadsSettingsInfo(
        showDownloadConfirmation = showDownloadConfirmation,
        downloads = downloadEntries,
      ),
      playbackSettings = PlaybackSettingsInfo(
        forwardTime = forwardTime.milliseconds,
        backwardTime = backwardTime.milliseconds,
        trackResetThreshold = trackResetThreshold,
        mp3IndexSeeking = mp3IndexSeeking,
        remoteNextPrevSkipsChapters = remoteNextPrevSkipsChapters,
        syncEnabled = syncEnabled,
        serverSessionsEnabled = serverSessionsEnabled,
        autoSyncEnabled = syncEnabled && autoSyncEnabled,
        playbackHistoryEnabled = playbackHistoryEnabled,
        autoRewindOnResumeEnabled = autoRewindOnResumeEnabled,
        resumeRewindConfig = resumeRewindConfig,
        resumeRewindPreview = resumeRewindPreview,
        autoRewindStopAtChapterBoundary = autoRewindStopAtChapterBoundary,
        bookTimeInPlaybackUi = bookTimeInPlaybackUi,
        playbackWavyScrubber = playbackWavyScrubber,
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
      socketSyncEnabled = socketSyncEnabled,
      aboutSettings = AboutSettingsInfo(
        crashReportingEnabled = crashReportingEnabled,
        analyticReportingEnabled = analyticReportingEnabled,
        showAppUpdateSignIn = appUpdateSignInDismissed && !appUpdateSignedIn,
      ),
      androidAutoSettings = AndroidAutoSettingsInfo(
        isAndroidAutoAvailable = isAndroidAutoAvailable,
        categories = androidAutoCategories,
      ),
      developerSettings = DeveloperSettingsInfo(
        developerModeEnabled = developerModeEnabled || applicationInfo.debugBuild,
        sessionAge = sessionAge,
        showWidgetPinningPrompt = showWidgetPinningPrompt,
        analyticsDebugState = analytics.debugState,
        mediaButtonPackages = mediaButtonPackages,
        fakeAppUpdateSignedIn = fakeAppUpdateSignedIn,
        fakeAppUpdateAvailable = fakeAppUpdateAvailable,
        fakeAppUpdateFailDownload = fakeAppUpdateFailDownload,
      ),
    ) { event ->
      analyticUiEventHandler.handle(event)
      when (event) {
        SettingsUiEvent.Back -> navigator.pop()
        is SettingsUiEvent.SettingsPaneClick -> navigator.goTo(SettingsScreen(event.pane.screenPage))

        is SettingsUiEvent.AccountSettingEvent -> when (event) {
          is ChangeName -> {
            scope.launch { serverRepository.changeName(event.name) }
          }

          is SettingsUiEvent.AccountSettingEvent.SocketSyncEnabled -> {
            settings.socketEnabled = event.enabled
          }

          Logout -> {
            scope.launch { accountManager.logout(server.dataOrNull!!) }
          }
        }

        is SettingsUiEvent.AppearanceSettingEvent -> when (event) {
          is Theme -> settings.themeMode = event.themeMode
          is DynamicItemDetailTheming -> themeSettings.dynamicallyThemeItemDetail = event.enabled
          is DynamicPlaybackTheming -> themeSettings.dynamicallyThemePlayback = event.enabled
          is ItemCardMarqueeEnabled -> settings.libraryItemMarqueeEnabled = event.enabled
          SettingsUiEvent.AppearanceSettingEvent.OpenThemeBuilder -> navigator.goTo(ThemePickerScreen)
        }

        is SettingsUiEvent.DownloadsSettingEvent -> when (event) {
          is ShowDownloadConfirmation -> settings.showConfirmDownload = event.enabled
          is DownloadClicked -> navigator.goTo(
            LibraryItemScreen(
              libraryItemId = event.entry.libraryItem.id,
              episodeId = (event.entry as? DownloadEntry.Episode)?.episode?.id,
            ),
          )
          is DeleteDownload -> when (val entry = event.entry) {
            is DownloadEntry.Book -> offlineDownloadManager.delete(entry.libraryItem)
            is DownloadEntry.Episode -> offlineDownloadManager.deleteEpisode(
              entry.libraryItem,
              entry.episode,
            )
          }
        }

        is SettingsUiEvent.PlaybackSettingEvent -> when (event) {
          is ForwardTime -> playbackSettings.forwardTimeMs = event.forwardTime.inWholeMilliseconds
          is BackwardTime -> playbackSettings.backwardTimeMs = event.backwardTime.inWholeMilliseconds
          is TrackResetThreshold -> playbackSettings.trackResetThreshold = event.trackResetThreshold
          is Mp3IndexSeeking -> playbackSettings.enableMp3IndexSeeking = event.mp3IndexSeeking
          is RemoteNextPrevSkipsChapters ->
            playbackSettings.remoteNextPrevSkipsChapters = event.remoteNextPrevSkipsChapters
          is SyncEnabled -> playbackSettings.syncEnabled = event.enabled
          is AutoSyncEnabled -> playbackSettings.autoSyncEnabled = event.enabled
          is ServerSessionsEnabled -> playbackSettings.serverSessionsEnabled = event.enabled
          is PlaybackHistoryEnabled -> {
            playbackSettings.playbackHistoryEnabled = event.enabled
            if (!event.enabled) {
              scope.launch { playbackHistoryRepository.clearAll() }
            }
          }
          is AutoRewindOnResumeEnabled -> playbackSettings.autoRewindOnResumeEnabled = event.enabled
          is MinPauseThreshold -> {
            playbackSettings.resumeRewindConfig =
              playbackSettings.resumeRewindConfig.copy(minPauseThreshold = event.threshold)
          }
          is ResumeRewindRange -> {
            playbackSettings.resumeRewindConfig = playbackSettings.resumeRewindConfig.copy(
              minRewind = event.minRewind,
              maxRewind = event.maxRewind,
            )
          }
          is AutoRewindStopAtChapterBoundary ->
            playbackSettings.autoRewindStopAtChapterBoundary = event.enabled
          is BookTimeInPlaybackUi -> playbackSettings.bookTimeInPlaybackUi = event.enabled
          is PlaybackWavyScrubber -> playbackSettings.playbackWavyScrubber = event.enabled
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
          ChangelogClick -> navigator.goTo(ChangelogScreen)
          AttributionsClick -> navigator.goTo(AttributionScreen)
          DeveloperClick -> navigator.goTo(UrlScreen(applicationUrls.developerHomepage))
          GithubClick -> navigator.goTo(UrlScreen(applicationUrls.githubDiscussion))
          PrivacyPolicyClick -> navigator.goTo(UrlScreen(applicationUrls.privacyPolicy))
          TermsOfServiceClick -> navigator.goTo(UrlScreen(applicationUrls.termsOfService))
          is SettingsUiEvent.AboutSettingEvent.AnalyticReportingEnabled -> {
            settings.analyticReportingEnabled = event.enabled
          }
          is SettingsUiEvent.AboutSettingEvent.CrashReportingEnabled -> {
            settings.crashReportingEnabled = event.enabled
          }
          SettingsUiEvent.AboutSettingEvent.AppUpdateSignInClick -> {
            scope.launch {
              appUpdateSource.signIn()
              if (appUpdateSource.isSignedIn()) {
                settings.appUpdateSignInDismissed = false
              }
              appUpdateInvalidator++
            }
          }
        }

        is SettingsUiEvent.DeveloperSettingEvent -> when (event) {
          is SettingsUiEvent.DeveloperSettingEvent.SessionAge -> devSettings.sessionAge = event.sessionAge
          is SettingsUiEvent.DeveloperSettingEvent.ShowWidgetPinningChange ->
            settings.hasShownWidgetPinning = event.enabled
          is SettingsUiEvent.DeveloperSettingEvent.EnableDeveloperMode -> devSettings.developerModeEnabled = true
          is SettingsUiEvent.DeveloperSettingEvent.ClearMediaButtonPackages -> devSettings.clearMediaButtonPackages()
          is SettingsUiEvent.DeveloperSettingEvent.InvalidateCurrentAccount -> {
            scope.launch {
              accountManager.invalidateAccount(userSession.requiredUser)
            }
          }
          is SettingsUiEvent.DeveloperSettingEvent.FakeAppUpdateSignedIn ->
            devSettings.fakeAppUpdateSignedIn = event.enabled
          is SettingsUiEvent.DeveloperSettingEvent.FakeAppUpdateAvailable ->
            devSettings.fakeAppUpdateAvailable = event.enabled
          is SettingsUiEvent.DeveloperSettingEvent.FakeAppUpdateFailDownload ->
            devSettings.fakeAppUpdateFailDownload = event.enabled
          is SettingsUiEvent.DeveloperSettingEvent.ResetAppUpdateDismissals -> {
            settings.appUpdateSignInDismissed = false
            settings.appUpdateDismissedVersionCode = 0L
            appUpdateInvalidator++
          }
        }

        is SettingsUiEvent.AndroidAutoSettingEvent -> when (event) {
          is SettingsUiEvent.AndroidAutoSettingEvent.OpenAndroidAutoSettings -> androidAuto.openSettings()
          is SettingsUiEvent.AndroidAutoSettingEvent.SetCategoryVisible ->
            androidAutoSettings.setCategoryVisible(event.category, event.visible)
          is SettingsUiEvent.AndroidAutoSettingEvent.SetCategoryGridLayout ->
            androidAutoSettings.setCategoryGridLayout(event.category, event.isGrid)
          is SettingsUiEvent.AndroidAutoSettingEvent.ReorderCategories ->
            androidAutoSettings.setCategoryOrder(event.order)
        }
      }
    }
  }
}
