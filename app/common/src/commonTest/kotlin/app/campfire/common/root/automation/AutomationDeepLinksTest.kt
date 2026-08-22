// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.root.automation

import app.campfire.audioplayer.PlaybackController
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.model.ServerStatus
import app.campfire.common.screens.HomeScreen
import app.campfire.common.screens.SettingsScreen
import app.campfire.common.screens.StatisticsScreen
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaType
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.navigation.DeepLink
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.test.TestCampfireSettings
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.screen.ThemePickerScreen
import app.campfire.whatsnew.api.Changelog
import app.campfire.whatsnew.api.WhatsNewRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime

class AutomationDeepLinksTest {

  @Test
  fun `resolveScreen maps known names`() {
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("home"))).isEqualTo(HomeScreen)
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("library"))).isEqualTo(LibraryScreen())
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("statistics"))).isEqualTo(StatisticsScreen)
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("theme_picker"))).isEqualTo(ThemePickerScreen)
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("library_item", "abc")))
      .isEqualTo(LibraryItemScreen("abc"))
  }

  @Test
  fun `resolveScreen maps settings pages case-insensitively and defaults to root`() {
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("settings", "appearance")))
      .isEqualTo(SettingsScreen(SettingsScreen.Page.Appearance))
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("settings", "nope")))
      .isEqualTo(SettingsScreen(SettingsScreen.Page.Root))
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("settings")))
      .isEqualTo(SettingsScreen(SettingsScreen.Page.Root))
  }

  @Test
  fun `resolveScreen returns null for unknown names or a missing item id`() {
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("bogus"))).isNull()
    assertThat(AutomationScreens.resolve(DeepLink.Navigate("library_item"))).isNull()
  }

  @Test
  fun `applySetup silences first-run prompts, applies theme, and authenticates when logged out`() = runTest {
    val auth = FakeAuthRepository()
    val settings = TestCampfireSettings(this)
    val themes = FakeAppThemeRepository()
    val whatsNew = FakeWhatsNewRepository()

    AutomationDeepLinks(auth, settings, themes, whatsNew).applySetup(
      setup = DeepLink.Setup("http://h", "Home", "demo", "pw", themeMode = "dark", theme = "forest"),
      isLoggedIn = false,
    )

    assertThat(settings.hasEverConsented).isTrue()
    assertThat(settings.hasShownWidgetPinning).isTrue()
    assertThat(settings.themeMode).isEqualTo(ThemeMode.DARK)
    assertThat(whatsNew.dismissed).isTrue()
    assertThat(themes.current).isEqualTo(AppTheme.Fixed.Forest)
    assertThat(auth.calls).isEqualTo(listOf(Triple("http://h", "demo", "pw")))
  }

  @Test
  fun `applySetup does not re-authenticate when already logged in and ignores unknown themes`() = runTest {
    val auth = FakeAuthRepository()
    val themes = FakeAppThemeRepository()

    AutomationDeepLinks(auth, TestCampfireSettings(this), themes, FakeWhatsNewRepository()).applySetup(
      setup = DeepLink.Setup("http://h", "Home", "demo", "pw", theme = "not-a-theme"),
      isLoggedIn = true,
    )

    assertThat(auth.calls).isEqualTo(emptyList())
    assertThat(themes.current).isNull()
  }

  @Test
  fun `selectLibrary picks by name case-insensitively and ignores unknown names`() = runTest {
    val audiobooks = library("1", "Audiobooks")
    val podcasts = library("2", "Podcasts")
    val repo = FakeLibraryRepository(listOf(audiobooks, podcasts))
    val playback = FakePlaybackController()
    val automation = UserAutomationDeepLinks(repo, playback, FakeSessionsRepository())

    automation.selectLibrary("podcasts")
    assertThat(repo.selected).isEqualTo(podcasts)

    automation.selectLibrary("Comics")
    assertThat(repo.selected).isEqualTo(podcasts)
  }

  @Test
  fun `play starts a session and stopPlayback stops the current one`() = runTest {
    val playback = FakePlaybackController()
    val sessions = FakeSessionsRepository()
    val automation = UserAutomationDeepLinks(FakeLibraryRepository(emptyList()), playback, sessions)

    automation.play("item-1")
    assertThat(playback.started).isEqualTo(listOf("item-1"))

    automation.stopPlayback()
    assertThat(playback.stopped).isEqualTo(emptyList())

    sessions.currentSession = sessionFor("item-1")
    automation.stopPlayback()
    assertThat(playback.stopped).isEqualTo(listOf("item-1"))
  }

  @OptIn(ExperimentalUuidApi::class)
  private fun sessionFor(itemId: String) = Session(
    id = Uuid.random(),
    libraryItem = libraryItem(id = itemId),
    userId = "user",
    isDeleted = false,
    playMethod = PlayMethod.DirectPlay,
    mediaPlayer = "exoplayer",
    timeListening = Duration.ZERO,
    startTime = Duration.ZERO,
    currentTime = Duration.ZERO,
    lastPlayedAt = null,
    startedAt = LocalDateTime(2026, 1, 1, 0, 0),
    updatedAt = LocalDateTime(2026, 1, 1, 0, 0),
  )

  private class FakePlaybackController : PlaybackController {
    val started = mutableListOf<String>()
    val stopped = mutableListOf<String>()

    override fun startSession(itemId: String, playImmediately: Boolean, chapterId: Int?, episodeId: PodcastEpisodeId?) {
      started += itemId
    }

    override fun stopSession(itemId: String, clearQueue: Boolean, episodeId: PodcastEpisodeId?) {
      stopped += itemId
    }
  }

  private fun library(id: LibraryId, name: String) = Library(
    id = id,
    name = name,
    displayOrder = 0,
    icon = Library.Icon.Database,
    mediaType = MediaType.Book,
    provider = "google",
    coverAspectRatio = 1,
    audiobooksOnly = false,
    createdAt = 0L,
    lastUpdate = 0L,
  )

  private class FakeAuthRepository : AuthRepository {
    val calls = mutableListOf<Triple<String, String, String>>()

    override suspend fun status(serverUrl: String, networkSettings: NetworkSettings?): Result<ServerStatus> =
      Result.failure(UnsupportedOperationException())

    override suspend fun authenticate(
      serverUrl: String,
      serverName: String,
      username: String,
      password: String,
      userId: UserId?,
      networkSettings: NetworkSettings?,
    ): Result<Unit> {
      calls += Triple(serverUrl, username, password)
      return Result.success(Unit)
    }

    override suspend fun authenticate(
      serverUrl: String,
      serverName: String,
      codeVerifier: String,
      code: String,
      state: String,
      userId: UserId?,
      networkSettings: NetworkSettings?,
    ): Result<Unit> = Result.failure(UnsupportedOperationException())

    override suspend fun getNetworkSettings(userId: UserId): NetworkSettings? = null
  }

  private class FakeAppThemeRepository : AppThemeRepository {
    var current: AppTheme? = null

    override fun observeCurrentAppTheme(): StateFlow<AppTheme> = MutableStateFlow(AppTheme.Fixed.Tent)
    override fun observeCustomThemes(): Flow<List<AppTheme.Fixed>> = flowOf(emptyList())
    override fun setCurrentTheme(theme: AppTheme) {
      current = theme
    }
    override suspend fun getCustomTheme(id: String): Result<AppTheme.Fixed> = Result.failure(
      UnsupportedOperationException(),
    )
    override suspend fun saveCustomTheme(theme: AppTheme.Fixed) = Unit
    override suspend fun deleteCustomTheme(id: String) = Unit
  }

  private class FakeWhatsNewRepository : WhatsNewRepository {
    var dismissed = false

    override suspend fun getChangelog(): Changelog = throw UnsupportedOperationException()
    override fun observeShouldShowWhatsNew(): Flow<Boolean> = flowOf(false)
    override suspend fun dismissWhatsNew() {
      dismissed = true
    }
  }

  private class FakeLibraryRepository(private val libraries: List<Library>) : LibraryRepository {
    var selected: Library? = null

    override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> = flowOf(libraries.first())
    override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> = flowOf(libraries)
    override fun observeCurrentLibraryItems(refresh: Boolean): Flow<List<LibraryItem>> = flowOf(emptyList())
    override fun createLibraryItemPager(
      user: User,
      filter: ContentFilter?,
      sortMode: ContentSortMode,
      sortDirection: SortDirection,
    ): LibraryItemPager = throw UnsupportedOperationException()
    override fun observeFilteredLibraryCount(
      filter: ContentFilter?,
      sortMode: ContentSortMode,
      sortDirection: SortDirection,
    ): Flow<Int?> = flowOf(null)
    override suspend fun setCurrentLibrary(library: Library) {
      selected = library
    }
    override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> =
      Result.failure(UnsupportedOperationException())
  }
}
