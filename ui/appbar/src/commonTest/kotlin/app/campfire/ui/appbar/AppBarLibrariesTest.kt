// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.appbar

import app.campfire.core.coroutines.CoroutineScopeHolder
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
import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AppBarLibrariesTest {

  private val books = library("books", order = 1)
  private val podcasts = library("podcasts", order = 0)

  @Test
  fun `holds the current library and every library in display order`() = runTest {
    val repository = FakeLibraryRepository(current = books, all = listOf(books, podcasts))
    val holder = AppBarLibraries(repository, CoroutineScopeHolder { backgroundScope })

    holder.state.test {
      assertEquals(AppBarLibrariesState(), awaitItem())
      assertEquals(AppBarLibrariesState(books, listOf(podcasts, books)), awaitItem())
    }
  }

  @Test
  fun `keeps the last libraries after every app bar goes away`() = runTest {
    val repository = FakeLibraryRepository(current = books, all = listOf(books, podcasts))
    val holder = AppBarLibraries(repository, CoroutineScopeHolder { backgroundScope })

    holder.state.test {
      skipItems(2)
    }
    // Past the stop timeout, sharing restarts but the value never goes back to empty
    advanceTimeBy(10_000)
    holder.state.test {
      assertEquals(AppBarLibrariesState(books, listOf(podcasts, books)), awaitItem())
      expectNoEvents()
    }
  }

  @Test
  fun `selecting a library makes it current`() = runTest {
    val repository = FakeLibraryRepository(current = books, all = listOf(books, podcasts))
    val holder = AppBarLibraries(repository, CoroutineScopeHolder { backgroundScope })

    holder.select(podcasts)
    runCurrent()

    assertEquals(podcasts, repository.current.value)
  }

  private class FakeLibraryRepository(current: Library, all: List<Library>) : LibraryRepository {
    val current = MutableStateFlow<Library?>(current)
    private val all = MutableStateFlow(all)

    override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> = current.filterNotNull()
    override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> = all
    override suspend fun setCurrentLibrary(library: Library) {
      current.value = library
    }

    override fun observeCurrentLibraryItems(refresh: Boolean): Flow<List<LibraryItem>> = TODO()
    override fun createLibraryItemPager(
      user: User,
      filter: ContentFilter?,
      sortMode: ContentSortMode,
      sortDirection: SortDirection,
    ): LibraryItemPager = TODO()
    override fun observeFilteredLibraryCount(
      filter: ContentFilter?,
      sortMode: ContentSortMode,
      sortDirection: SortDirection,
    ): Flow<Int?> = TODO()
    override fun observeUnfinishedEpisodes(
      filter: ContentFilter?,
      sortMode: ContentSortMode,
      sortDirection: SortDirection,
    ): Flow<Map<LibraryItemId, Int>> = TODO()
    override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> = TODO()
  }

  private fun library(id: String, order: Int) = Library(
    id = id,
    name = id,
    displayOrder = order,
    icon = Library.Icon.Database,
    mediaType = MediaType.Book,
    provider = "audible",
    coverAspectRatio = 1,
    audiobooksOnly = false,
    createdAt = 0L,
    lastUpdate = 0L,
  )
}
