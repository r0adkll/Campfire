package app.campfire.libraries.ui.detail

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.common.test.coroutines.TestDispatcherProvider
import app.campfire.common.test.user
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.session.UserSession
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.libraries.test.FakeLibraryItemValidator
import app.campfire.libraries.ui.detail.book.BookPresenter
import app.campfire.libraries.ui.detail.podcast.PodcastPresenter
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import app.campfire.series.test.FakeSeriesRepository
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.TestCampfireSettings
import app.campfire.settings.test.TestThemeSettings
import app.campfire.ui.theming.test.FakeThemeManager
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeUserRepository
import com.slack.circuit.test.FakeNavigator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

internal const val TestLibraryItemId = "item_id"

abstract class BaseLibraryItemPresenterTest {

  internal val screen = LibraryItemScreen(TestLibraryItemId)
  internal val navigator = FakeNavigator(screen)
  internal val libraryItemRepository = FakeLibraryItemRepository()
  internal val libraryItemValidator = FakeLibraryItemValidator()
  internal val seriesRepository = FakeSeriesRepository()
  internal val sessionsRepository = FakeSessionsRepository()
  internal val sessionQueue = FakeSessionQueue()
  internal val mediaProgressRepository = FakeMediaProgressRepository()
  internal val userRepository = FakeUserRepository()
  internal val playbackHistoryRepository = FakePlaybackHistoryRepository()
  internal val playbackController = FakePlaybackController()
  internal val audioPlayerHolder = FakeAudioPlayerHolder()
  internal val audioPlayer = FakeAudioPlayer()
  internal val offlineDownloadManager = FakeOfflineDownloadManager()
  internal val themeManager = FakeThemeManager()
  internal val themeSettings = TestThemeSettings()
  internal val settings = TestCampfireSettings()
  internal val analytics = FakeAnalytics()
  internal val dispatcherProvider = TestDispatcherProvider()

  internal val bookPresenter = BookPresenter(
    validator = libraryItemValidator,
    seriesRepository = seriesRepository,
    sessionsRepository = sessionsRepository,
    sessionQueue = sessionQueue,
    mediaProgressRepository = mediaProgressRepository,
    playbackHistoryRepository = playbackHistoryRepository,
    playbackController = playbackController,
    audioPlayerHolder = audioPlayerHolder,
    offlineDownloadManager = offlineDownloadManager,
    settings = settings,
    analytics = analytics,
    themeManager = themeManager,
    addToPlaylistDialog = AddToPlaylistDialog.NoOp,
    dispatcherProvider = dispatcherProvider,
  )

  internal val remoteEpisodeDownloadTracker: RemoteEpisodeDownloadTracker =
    EmptyRemoteEpisodeDownloadTracker

  internal val podcastPresenter = PodcastPresenter(
    analytics = analytics,
    userRepository = userRepository,
    sessionsRepository = sessionsRepository,
    mediaProgressRepository = mediaProgressRepository,
    playbackHistoryRepository = playbackHistoryRepository,
    playbackController = playbackController,
    offlineDownloadManager = offlineDownloadManager,
    settings = settings,
    addToPlaylistDialog = AddToPlaylistDialog.NoOp,
    remoteEpisodeDownloadTracker = remoteEpisodeDownloadTracker,
  )

  internal val presenter = LibraryItemPresenter(
    userSession = UserSession.LoggedIn(user("user_id")),
    screen = screen,
    navigator = navigator,
    repository = libraryItemRepository,
    bookPresenter = bookPresenter,
    podcastPresenter = podcastPresenter,
    themeManager = themeManager,
    themeSettings = themeSettings,
    dispatcherProvider = dispatcherProvider,
    offlineDownloadManager = offlineDownloadManager,
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

/**
 * A no-op tracker used by tests that don't care about the remote download queue. Always reports
 * an empty state. Tests that need to assert against the queue can replace this with a real fake.
 */
private object EmptyRemoteEpisodeDownloadTracker : RemoteEpisodeDownloadTracker {
  override val state: StateFlow<Map<LibraryItemId, List<RemoteEpisodeDownload>>> =
    MutableStateFlow(emptyMap())
  override val recentlyFinishedUrls: StateFlow<Set<String>> = MutableStateFlow(emptySet())
  override fun observe(libraryItemId: LibraryItemId): Flow<List<RemoteEpisodeDownload>> =
    flowOf(emptyList())
}
