// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.core.model.User
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.model.Shelf
import app.campfire.home.api.model.ShelfId
import app.campfire.home.api.model.ShelfIds
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import app.campfire.settings.api.ThemeKey
import app.campfire.settings.api.ThemeMode
import app.campfire.settings.api.ThemeSettings
import app.campfire.settings.test.TestLibraryViewSettings
import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest

class DefaultPreviewContentSourceTest {

  private val home = FakeHomeRepository()
  private val libraries = FakeLibraryRepository()
  private val layout = TestLibraryViewSettings().apply {
    libraryItemDisplayState = ItemDisplayState.Grid
    librarySortMode = ContentSortMode.Title
    librarySortDirection = SortDirection.Ascending
  }
  private val theme = FakeThemeSettings()

  private val source = DefaultPreviewContentSource(home, libraries, layout, theme)

  @Test
  fun theUsersShelvesAndLibraryComeThrough() = runTest {
    source.observe().test {
      val library = awaitLibrary { !it.isEmpty }

      assertThat(library).all {
        prop(PreviewLibrary::continueListening).all {
          prop(PreviewShelf::label).isEqualTo("Continue Listening")
          prop(PreviewShelf::items).transform { items -> items.map { it.item.id } }.containsExactly("li_hail_mary")
        }
        prop(PreviewLibrary::recentlyAdded).prop(PreviewShelf::items)
          .transform { items -> items.map { it.item.id } }
          .containsExactly("li_circe", "li_dune")
        prop(PreviewLibrary::itemCount).isEqualTo(65)
        prop(PreviewLibrary::isPodcastLibrary).isFalse()
        prop(PreviewLibrary::displayState).isEqualTo(ItemDisplayState.Grid)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun anEmptyLibrary_showsSamplesInTheUsersLayout() = runTest {
    home.shelfEntities.clear()
    layout.libraryItemDisplayState = ItemDisplayState.List

    source.observe().test {
      val library = awaitItem().library

      assertThat(library.continueListening.items.first().item.media.metadata.title).isEqualTo("Moby-Dick")
      assertThat(library.displayState).isEqualTo(ItemDisplayState.List)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun aFailingSource_leavesTheRestOfTheLibrary() = runTest {
    libraries.countFails = true
    home.failingShelf = stored(ShelfIds.RecentlyAdded)

    source.observe().test {
      val library = awaitLibrary { !it.isEmpty }

      assertThat(library.continueListening.items.map { it.item.id }).containsExactly("li_hail_mary")
      assertThat(library.recentlyAdded.items).transform { it.isEmpty() }.isTrue()
      assertThat(library.itemCount).isNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun aPodcastLibrary_usesItsRecentEpisodesShelf() = runTest {
    libraries.mediaType = MediaType.Podcast
    home.shelves.value = FeedResponse.Success(
      listOf(
        Shelf(stored(ShelfIds.ContinueListening), "Continue Listening", total = 1, type = ShelfType.EPISODE, order = 0),
        Shelf(
          id = stored(ShelfIds.EpisodesRecentlyAdded),
          label = "Newest Episodes",
          total = 2,
          type = ShelfType.EPISODE,
          order = 1,
        ),
      ),
    )
    home.shelfEntities[stored(ShelfIds.EpisodesRecentlyAdded)] = listOf(previewBook("li_show", "A Show", "Host").item)

    source.observe().test {
      val library = awaitLibrary { it.isPodcastLibrary && it.recentlyAdded.items.isNotEmpty() }

      assertThat(library.recentlyAdded.label).isEqualTo("Newest Episodes")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun playerUsesCoverColors_followsThePlaybackSetting() = runTest {
    source.observe().test {
      assertThat(awaitItem().playerUsesCoverColors).isTrue()

      theme.playback.value = false

      var content = awaitItem()
      while (content.playerUsesCoverColors) content = awaitItem()
      assertThat(content.playerUsesCoverColors).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  private suspend fun app.cash.turbine.ReceiveTurbine<PreviewContent>.awaitLibrary(
    predicate: (PreviewLibrary) -> Boolean,
  ): PreviewLibrary {
    while (true) {
      val library = awaitItem().library
      if (predicate(library)) return library
    }
  }
}

/** How the home store keys a server shelf: with the user and library after the server's id. */
private fun stored(serverId: String): ShelfId = serverId + "_user_library"

private class FakeHomeRepository : HomeRepository {
  val shelves = MutableStateFlow<FeedResponse<List<Shelf>>>(
    FeedResponse.Success(
      listOf(
        Shelf(stored(ShelfIds.ContinueListening), "Continue Listening", total = 1, type = ShelfType.BOOK, order = 0),
        Shelf(stored(ShelfIds.RecentlyAdded), "Recently Added", total = 2, type = ShelfType.BOOK, order = 1),
      ),
    ),
  )
  val shelfEntities = mutableMapOf<ShelfId, List<ShelfEntity>>(
    stored(ShelfIds.ContinueListening) to listOf(previewBook("li_hail_mary", "Project Hail Mary", "Andy Weir").item),
    stored(ShelfIds.RecentlyAdded) to listOf(
      previewBook("li_circe", "Circe", "Madeline Miller").item,
      previewBook("li_dune", "Dune", "Frank Herbert").item,
    ),
  )
  var failingShelf: ShelfId? = null

  override fun observeHomeFeed(): Flow<FeedResponse<List<Shelf>>> = shelves

  override suspend fun refreshHomeFeed() = Unit

  override fun observeMediaProgress(libraryItemIds: Set<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>> =
    MutableStateFlow(emptyMap())

  override fun observeShelf(shelfId: ShelfId, shelfType: ShelfType): Flow<List<ShelfEntity>> = flow {
    if (shelfId == failingShelf) error("The $shelfId shelf won't load")
    emit(shelfEntities[shelfId].orEmpty())
  }
}

private class FakeLibraryRepository : LibraryRepository {
  var mediaType = MediaType.Book
  var countFails = false

  override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> = MutableStateFlow(
    Library(
      id = "library",
      name = "Library",
      displayOrder = 0,
      icon = Library.Icon.Database,
      mediaType = mediaType,
      provider = "",
      coverAspectRatio = 1,
      audiobooksOnly = false,
      createdAt = 0L,
      lastUpdate = 0L,
    ),
  )

  override fun observeFilteredLibraryCount(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Int?> = flow {
    if (countFails) error("The count won't load")
    emit(65)
  }

  override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> = TODO()
  override fun observeCurrentLibraryItems(refresh: Boolean): Flow<List<LibraryItem>> = TODO()
  override fun createLibraryItemPager(
    user: User,
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): LibraryItemPager = TODO()
  override fun observeUnfinishedEpisodes(
    filter: ContentFilter?,
    sortMode: ContentSortMode,
    sortDirection: SortDirection,
  ): Flow<Map<LibraryItemId, Int>> = TODO()
  override suspend fun setCurrentLibrary(library: Library) = TODO()
  override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> = TODO()
}

private class FakeThemeSettings : ThemeSettings {
  val itemDetail = MutableStateFlow(true)
  val playback = MutableStateFlow(true)

  override var dynamicallyThemeItemDetail: Boolean
    get() = itemDetail.value
    set(value) {
      itemDetail.value = value
    }
  override fun observeDynamicallyThemeItemDetail(): StateFlow<Boolean> = itemDetail

  override var dynamicallyThemePlayback: Boolean
    get() = playback.value
    set(value) {
      playback.value = value
    }
  override fun observeDynamicallyThemePlayback(): StateFlow<Boolean> = playback

  override var themeId: ThemeKey = ThemeKey.Tent
  override var themeMode: ThemeMode = ThemeMode.SYSTEM
  override fun observeTheme(): StateFlow<ThemeMode> = MutableStateFlow(themeMode)
}
