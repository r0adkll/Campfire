package app.campfire.libraries.ui.detail

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.common.test.coroutines.TestDispatcherProvider
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.preview.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.series.test.FakeSeriesRepository
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.TestCampfireSettings
import app.campfire.settings.test.TestThemeSettings
import app.campfire.ui.theming.test.FakeThemeManager
import app.campfire.user.test.FakeMediaProgressRepository
import com.slack.circuit.test.FakeNavigator

internal const val TestLibraryItemId = "item_id"

abstract class BaseLibraryItemPresenterTest {

  internal val screen = LibraryItemScreen(TestLibraryItemId)
  internal val navigator = FakeNavigator(screen)
  internal val libraryItemRepository = FakeLibraryItemRepository()
  internal val seriesRepository = FakeSeriesRepository()
  internal val sessionsRepository = FakeSessionsRepository()
  internal val mediaProgressRepository = FakeMediaProgressRepository()
  internal val playbackController = FakePlaybackController()
  internal val audioPlayerHolder = FakeAudioPlayerHolder()
  internal val audioPlayer = FakeAudioPlayer()
  internal val offlineDownloadManager = FakeOfflineDownloadManager()
  internal val themeManager = FakeThemeManager()
  internal val themeSettings = TestThemeSettings()
  internal val settings = TestCampfireSettings()
  internal val analytics = FakeAnalytics()
  internal val dispatcherProvider = TestDispatcherProvider()

  protected val presenter = LibraryItemPresenter(
    screen = screen,
    navigator = navigator,
    repository = libraryItemRepository,
    seriesRepository = seriesRepository,
    sessionsRepository = sessionsRepository,
    mediaProgressRepository = mediaProgressRepository,
    playbackController = playbackController,
    audioPlayerHolder = audioPlayerHolder,
    offlineDownloadManager = offlineDownloadManager,
    settings = settings,
    analytics = analytics,
    themeManager = themeManager,
    themeSettings = themeSettings,
    dispatcherProvider = dispatcherProvider,
  )
}

internal fun emptyLibraryItem(
  id: LibraryItemId = TestLibraryItemId,
  description: String? = null,
  publisher: String? = null,
  publishedYear: String? = null,
  genres: List<String> = emptyList(),
  tags: List<String> = emptyList(),
  numOfChapters: Int = 0,
) = libraryItem(
  id = id,
  description = description,
  publisher = publisher,
  publishedYear = publishedYear,
  genres = genres,
  tags = tags,
  numOfChapters = numOfChapters,
)
