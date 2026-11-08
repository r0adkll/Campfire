// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.paged

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withCompositionLocal
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.campfire.analytics.test.FakeAnalytics
import app.campfire.common.test.mediaProgress
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaType
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.LibraryItemSortModes
import app.campfire.core.settings.PodcastSortModes
import app.campfire.core.settings.SortDirection
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.offline.test.FakeDownloads
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.RemoteEpisodeDownloadTracker
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeUserRepository
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.slack.circuit.retained.LocalRetainedStateRegistry
import com.slack.circuit.retained.RetainedStateRegistry
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryPresenterTest {

  private val userRepository = FakeUserRepository()
  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val repository = RecordingLibraryRepository()
  private val settings = TestLibraryViewSettings().apply {
    librarySortMode = ContentSortMode.AuthorFL
    librarySortDirection = SortDirection.Ascending
    podcastSortMode = ContentSortMode.Title
    podcastSortDirection = SortDirection.Ascending
  }

  private val navigator = FakeNavigator(LibraryScreen())
  private val serverDownloads = MutableStateFlow<Map<LibraryItemId, List<RemoteEpisodeDownload>>>(emptyMap())

  private val presenter = LibraryPresenter(
    screen = LibraryScreen(),
    navigator = navigator,
    userRepository = userRepository,
    repository = repository,
    downloads = FakeDownloads(),
    mediaProgressRepository = mediaProgressRepository,
    remoteEpisodeDownloadTracker = object : RemoteEpisodeDownloadTracker {
      override val state = serverDownloads
      override val recentlyFinishedUrls = MutableStateFlow(emptySet<String>())
      override fun observe(libraryItemId: LibraryItemId) = TODO()
    },
    libraryViewSettings = settings,
    analytics = FakeAnalytics(),
  )

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `a book library pages with the book sort`() = runTest {
    repository.currentLibrary.emit(library(MediaType.Book))

    presenter.test {
      val state = expectMostRecentItem()

      assertThat(state.sort).isEqualTo(
        LibrarySort(ContentSortMode.AuthorFL, SortDirection.Ascending, LibraryItemSortModes),
      )
      assertThat(repository.pagedSorts).containsExactly(ContentSortMode.AuthorFL to SortDirection.Ascending)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a podcast library counts the episodes the server is downloading`() = runTest {
    serverDownloads.value = mapOf(
      "podcast_a" to listOf(serverDownload("1", "podcast_a"), serverDownload("2", "podcast_a")),
      "podcast_b" to listOf(serverDownload("3", "podcast_b")),
    )
    repository.currentLibrary.emit(library(MediaType.Podcast))

    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.serverDownloadCount).isEqualTo(3)

      state.eventSink(LibraryUiEvent.ServerDownloadsClick)
      assertThat(navigator.awaitNextScreen()).isEqualTo(PodcastDownloadQueueScreen)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the cards follow each book's progress as it changes, not as its page has it`() = runTest {
    repository.currentLibrary.emit(library(MediaType.Book))
    val listening = mediaProgress("book_a", progress = 0.42f)
    mediaProgressRepository.allProgressFlow.value = listOf(
      listening,
      // An episode's progress isn't its show's
      mediaProgress("podcast_a", episodeId = "episode_1", progress = 0.9f),
    )

    presenter.test {
      assertThat(expectMostRecentItem().progress).isEqualTo(mapOf("book_a" to listening))

      val finished = listening.copy(progress = 1f, isFinished = true)
      mediaProgressRepository.allProgressFlow.value = listOf(finished)
      assertThat(awaitItem().progress).isEqualTo(mapOf("book_a" to finished))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `listening on doesn't change the cards until the percent does`() = runTest {
    repository.currentLibrary.emit(library(MediaType.Book))
    val listening = mediaProgress("book_a", progress = 0.421f)
    mediaProgressRepository.allProgressFlow.value = listOf(listening)

    presenter.test {
      assertThat(expectMostRecentItem().progress).isEqualTo(mapOf("book_a" to listening))

      mediaProgressRepository.allProgressFlow.value = listOf(listening.copy(progress = 0.424f))
      expectNoEvents()

      val further = listening.copy(progress = 0.43f)
      mediaProgressRepository.allProgressFlow.value = listOf(further)
      assertThat(awaitItem().progress).isEqualTo(mapOf("book_a" to further))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a card shows its page's progress until the progress is known, then what's known`() {
    val item = libraryItem(id = "book_a")
    val paged = mediaProgress("book_a", progress = 0.2f)
    val withPagedProgress = item.copy(userMediaProgress = paged)

    assertThat(null.progressOf(withPagedProgress)).isEqualTo(paged)
    // Progress discarded since the page loaded
    assertThat(emptyMap<String, app.campfire.core.model.MediaProgress>().progressOf(withPagedProgress))
      .isEqualTo(null)
  }

  @Test
  fun `a book library has no server downloads`() = runTest {
    serverDownloads.value = mapOf("podcast_a" to listOf(serverDownload("1", "podcast_a")))
    repository.currentLibrary.emit(library(MediaType.Book))

    presenter.test {
      assertThat(expectMostRecentItem().serverDownloadCount).isEqualTo(0)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a podcast library pages with the podcast sort`() = runTest {
    settings.podcastSortMode = ContentSortMode.NumberOfEpisodes
    settings.podcastSortDirection = SortDirection.Descending
    repository.currentLibrary.emit(library(MediaType.Podcast))

    presenter.test {
      val state = expectMostRecentItem()

      assertThat(state.sort).isEqualTo(
        LibrarySort(ContentSortMode.NumberOfEpisodes, SortDirection.Descending, PodcastSortModes),
      )
      assertThat(state.totalItemCount).isEqualTo(12)
      assertThat(repository.pagedSorts)
        .containsExactly(ContentSortMode.NumberOfEpisodes to SortDirection.Descending)
      assertThat(repository.countedSorts)
        .containsExactly(ContentSortMode.NumberOfEpisodes to SortDirection.Descending)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `nothing is paged or counted until the selected library has loaded`() = runTest {
    presenter.test {
      assertThat(expectMostRecentItem().totalItemCount).isEqualTo(INVALID_ITEM_COUNT)

      assertThat(repository.pagedSorts).isEmpty()
      assertThat(repository.countedSorts).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `nothing is paged while the loaded library is not the selected one`() = runTest {
    repository.currentLibrary.emit(library(MediaType.Podcast, id = "previous_library_id"))

    presenter.test {
      expectMostRecentItem()

      assertThat(repository.pagedSorts).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `picking a sort in a podcast library keeps the book sort`() = runTest {
    repository.currentLibrary.emit(library(MediaType.Podcast))

    presenter.test {
      expectMostRecentItem().eventSink(LibraryUiEvent.SortModeSelected(ContentSortMode.FileModified))

      assertThat(expectMostRecentItem().sort.mode).isEqualTo(ContentSortMode.FileModified)
      assertThat(settings.podcastSortMode).isEqualTo(ContentSortMode.FileModified)
      assertThat(settings.librarySortMode).isEqualTo(ContentSortMode.AuthorFL)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `picking the current sort in a podcast library flips its direction`() = runTest {
    repository.currentLibrary.emit(library(MediaType.Podcast))

    presenter.test {
      expectMostRecentItem().eventSink(LibraryUiEvent.SortModeSelected(ContentSortMode.Title))

      assertThat(expectMostRecentItem().sort.direction).isEqualTo(SortDirection.Descending)
      assertThat(settings.librarySortDirection).isEqualTo(SortDirection.Ascending)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `picking a sort in a book library keeps the podcast sort`() = runTest {
    repository.currentLibrary.emit(library(MediaType.Book))

    presenter.test {
      expectMostRecentItem().eventSink(LibraryUiEvent.SortModeSelected(ContentSortMode.AuthorFL))

      assertThat(expectMostRecentItem().sort.direction).isEqualTo(SortDirection.Descending)
      assertThat(settings.librarySortDirection).isEqualTo(SortDirection.Descending)
      assertThat(settings.podcastSortDirection).isEqualTo(SortDirection.Ascending)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a podcast library badges each show's unfinished episodes`() = runTest {
    repository.unfinishedEpisodes.value = mapOf("pod_1" to 4)
    repository.currentLibrary.emit(library(MediaType.Podcast))

    presenter.test {
      val state = expectMostRecentItem()

      assertThat(state.isPodcastLibrary).isEqualTo(true)
      assertThat(state.unfinishedEpisodes).isEqualTo(mapOf("pod_1" to 4))
      // Counted under the podcast sort, the same pages the grid shows
      assertThat(repository.unfinishedEpisodeSorts).containsExactly(ContentSortMode.Title to SortDirection.Ascending)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a book library doesn't count episodes`() = runTest {
    repository.unfinishedEpisodes.value = mapOf("pod_1" to 4)
    repository.currentLibrary.emit(library(MediaType.Book))

    presenter.test {
      val state = expectMostRecentItem()

      assertThat(repository.pagedSorts).containsExactly(ContentSortMode.AuthorFL to SortDirection.Ascending)
      assertThat(state.isPodcastLibrary).isEqualTo(false)
      assertThat(state.unfinishedEpisodes).isEmpty()
      assertThat(repository.unfinishedEpisodeSorts).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `returning to the library keeps its pages instead of paging from the top`() = runTest {
    repository.currentLibrary.emit(library(MediaType.Book))
    val registry = RetainedStateRegistry()
    var showing by mutableStateOf(true)

    moleculeFlow(RecompositionMode.Immediate) {
      withCompositionLocal(LocalRetainedStateRegistry provides registry) {
        if (showing) presenter.present() else null
      }
    }.test {
      val before = expectMostRecentItem()!!

      // Opening an item: the navigator retains the screen's state, then removes it
      registry.saveAll()
      showing = false
      assertThat(expectMostRecentItem()).isNull()

      // Back again: the very first state must reuse the retained pages, or the list lays out
      // empty and loses its scroll position
      showing = true
      val returned = awaitItem()!!
      assertThat(returned.lazyPagingItems.loadState).isEqualTo(before.lazyPagingItems.loadState)
      assertThat(returned.totalItemCount).isEqualTo(12)

      expectNoEvents()
      assertThat(repository.pagedSorts).containsExactly(ContentSortMode.AuthorFL to SortDirection.Ascending)
      assertThat(repository.countedSorts).containsExactly(ContentSortMode.AuthorFL to SortDirection.Ascending)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun library(
    mediaType: MediaType,
    id: LibraryId = userRepository.userFlow.value.selectedLibraryId,
  ) = Library(
    id = id,
    name = "Library",
    displayOrder = 0,
    icon = Library.Icon.Database,
    mediaType = mediaType,
    provider = "",
    coverAspectRatio = 0,
    audiobooksOnly = false,
    createdAt = 0L,
    lastUpdate = 0L,
  )
}

private class RecordingLibraryRepository : LibraryRepository {

  val currentLibrary = MutableSharedFlow<Library>(replay = 1)
  val pagedSorts = mutableListOf<Pair<ContentSortMode, SortDirection>>()
  val countedSorts = mutableListOf<Pair<ContentSortMode, SortDirection>>()
  val unfinishedEpisodes = MutableStateFlow<Map<LibraryItemId, Int>>(emptyMap())
  val unfinishedEpisodeSorts = mutableListOf<Pair<ContentSortMode, SortDirection>>()

  override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> = currentLibrary
  override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> = flowOf(emptyList())
  override fun observeCurrentLibraryItems(refresh: Boolean): Flow<List<LibraryItem>> = flowOf(emptyList())

  override fun createLibraryItemPager(
    user: User,
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): LibraryItemPager {
    pagedSorts += sortMode to sortDirection
    return Pager(PagingConfig(pageSize = 20)) { EmptyPagingSource() }
  }

  override fun observeFilteredLibraryCount(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?> {
    countedSorts += sortMode to sortDirection
    return flowOf(12)
  }

  override fun observeUnfinishedEpisodes(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Map<LibraryItemId, Int>> {
    unfinishedEpisodeSorts += sortMode to sortDirection
    return unfinishedEpisodes
  }

  override suspend fun setCurrentLibrary(library: Library) = Unit

  override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> =
    throw NotImplementedError("not exercised in these tests")
}

private class EmptyPagingSource : PagingSource<Int, LibraryItem>() {
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LibraryItem> =
    LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

  override fun getRefreshKey(state: PagingState<Int, LibraryItem>): Int? = null
}

private fun serverDownload(id: String, libraryItemId: LibraryItemId) = RemoteEpisodeDownload(
  id = id,
  libraryItemId = libraryItemId,
  libraryId = null,
  url = "https://example.com/$id.mp3",
  episodeDisplayTitle = null,
  podcastTitle = null,
  state = RemoteEpisodeDownload.State.Queued,
  createdAt = null,
  startedAt = null,
)
