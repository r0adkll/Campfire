// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.paged

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.campfire.analytics.test.FakeAnalytics
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaType
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.LibraryItemSortModes
import app.campfire.core.settings.PodcastSortModes
import app.campfire.core.settings.SortDirection
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.offline.test.FakeDownloads
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.test.FakeUserRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryPresenterTest {

  private val userRepository = FakeUserRepository()
  private val repository = RecordingLibraryRepository()
  private val settings = TestLibraryViewSettings().apply {
    librarySortMode = ContentSortMode.AuthorFL
    librarySortDirection = SortDirection.Ascending
    podcastSortMode = ContentSortMode.Title
    podcastSortDirection = SortDirection.Ascending
  }

  private val presenter = LibraryPresenter(
    screen = LibraryScreen(),
    navigator = FakeNavigator(LibraryScreen()),
    userRepository = userRepository,
    repository = repository,
    downloads = FakeDownloads(),
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

  override suspend fun setCurrentLibrary(library: Library) = Unit

  override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> =
    throw NotImplementedError("not exercised in these tests")
}

private class EmptyPagingSource : PagingSource<Int, LibraryItem>() {
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LibraryItem> =
    LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

  override fun getRefreshKey(state: PagingState<Int, LibraryItem>): Int? = null
}
