// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.paged

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.campfire.analytics.test.FakeAnalytics
import app.campfire.common.test.user
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaType
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.offline.test.FakeDownloads
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.test.FakeUserRepository
import assertk.assertThat
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryPresenterTest {

  private val repository = CountingLibraryRepository()

  // The presenter collects its pager as LazyPagingItems, which loads on the main dispatcher
  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `a podcast library badges each show's unfinished episodes`() = runTest {
    repository.library.value = library(MediaType.Podcast)
    repository.unfinishedEpisodes.value = mapOf("pod_1" to 4)

    presenter().test {
      val state = expectMostRecentItemMatching { it.unfinishedEpisodes.isNotEmpty() }
      assertThat(state.unfinishedEpisodes).isEqualTo(mapOf("pod_1" to 4))
      assertThat(state.isPodcastLibrary).isEqualTo(true)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a book library doesn't count episodes`() = runTest {
    repository.library.value = library(MediaType.Book)
    repository.unfinishedEpisodes.value = mapOf("pod_1" to 4)

    presenter().test {
      testScheduler.advanceUntilIdle()
      val state = expectMostRecentItem()
      assertThat(state.unfinishedEpisodes).isEmpty()
      assertThat(state.isPodcastLibrary).isEqualTo(false)
      cancelAndIgnoreRemainingEvents()
    }
    assertThat(repository.observedLibrary).isEqualTo(true)
    assertThat(repository.unfinishedEpisodesRequests).isEqualTo(0)
  }

  private fun presenter() = LibraryPresenter(
    screen = LibraryScreen(),
    navigator = FakeNavigator(LibraryScreen()),
    userRepository = FakeUserRepository().apply { currentStatefulUserFlow.value = TestUser },
    repository = repository,
    downloads = FakeDownloads(),
    libraryViewSettings = TestLibraryViewSettings(),
    analytics = FakeAnalytics(),
  )

  private fun library(mediaType: MediaType) = Library(
    id = TestUser.selectedLibraryId,
    name = "Library",
    displayOrder = 0,
    icon = Library.Icon.Database,
    mediaType = mediaType,
    provider = "",
    coverAspectRatio = 1,
    audiobooksOnly = false,
    createdAt = 0,
    lastUpdate = 0,
  )

  private companion object {
    val TestUser = user("user")
  }
}

private suspend fun <T> app.cash.turbine.ReceiveTurbine<T>.expectMostRecentItemMatching(
  predicate: (T) -> Boolean,
): T {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}

private class CountingLibraryRepository : LibraryRepository {
  val library = MutableStateFlow<Library?>(null)
  val unfinishedEpisodes = MutableStateFlow<Map<LibraryItemId, Int>>(emptyMap())
  var unfinishedEpisodesRequests = 0

  var observedLibrary = false

  override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> {
    observedLibrary = true
    return flowOf(library.value!!)
  }

  override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> = flowOf(emptyList())

  override fun observeCurrentLibraryItems(refresh: Boolean): Flow<List<LibraryItem>> = flowOf(emptyList())

  override fun createLibraryItemPager(
    user: User,
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): LibraryItemPager = Pager(PagingConfig(pageSize = 50)) { EmptyPagingSource() }

  override fun observeFilteredLibraryCount(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?> = flowOf(null)

  override fun observeUnfinishedEpisodes(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Map<LibraryItemId, Int>> {
    unfinishedEpisodesRequests++
    return unfinishedEpisodes
  }

  override suspend fun setCurrentLibrary(library: Library) = Unit

  override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> =
    error("not used")
}

private class EmptyPagingSource : PagingSource<Int, LibraryItem>() {
  override fun getRefreshKey(state: PagingState<Int, LibraryItem>): Int? = null
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LibraryItem> =
    LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
}
