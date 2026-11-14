// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.downloads

import androidx.paging.Pager
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.Library
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaType
import app.campfire.core.model.User
import app.campfire.core.session.UserSession
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection
import app.campfire.libraries.api.AddPodcastContext
import app.campfire.libraries.api.LibraryFolder
import app.campfire.libraries.api.LibraryRepository
import app.campfire.libraries.api.paging.LibraryItemPager
import app.campfire.podcasts.api.EpisodeDownloadsSnapshot
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastFeedDetails
import app.campfire.podcasts.api.PodcastSearchResult
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.RemotePodcastEpisode
import app.campfire.user.test.fixtures.user
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class EpisodeDownloadsHydratorTest {

  private val currentLibrary = MutableStateFlow<Library?>(null)
  private val fetched = mutableListOf<LibraryId>()

  @Test
  fun `a podcast library loads the server's downloads`() = hydratorTest {
    currentLibrary.value = library("podcasts", MediaType.Podcast)

    assertThat(fetched).containsExactly("podcasts")
  }

  @Test
  fun `a book library has none to load`() = hydratorTest {
    currentLibrary.value = library("books", MediaType.Book)

    assertThat(fetched).isEmpty()
  }

  @Test
  fun `switching podcast libraries loads each once`() = hydratorTest {
    currentLibrary.value = library("first", MediaType.Podcast)
    currentLibrary.value = library("first", MediaType.Podcast).copy(name = "Renamed")
    currentLibrary.value = library("books", MediaType.Book)
    currentLibrary.value = library("second", MediaType.Podcast)

    assertThat(fetched).containsExactly("first", "second")
  }

  @Test
  fun `signed out there is nothing to load`() = hydratorTest(UserSession.LoggedOut) {
    currentLibrary.value = library("podcasts", MediaType.Podcast)

    assertThat(fetched).isEmpty()
  }

  private fun hydratorTest(
    userSession: UserSession = UserSession.LoggedIn(user("user")),
    block: suspend TestScope.() -> Unit,
  ) = runTest(UnconfinedTestDispatcher()) {
    EpisodeDownloadsHydrator(
      userSession = userSession,
      libraryRepository = Libraries(),
      podcastsRepository = Podcasts(),
      coroutineScopeHolder = CoroutineScopeHolder { backgroundScope },
    ).onCreate()
    block()
  }

  private fun library(id: LibraryId, mediaType: MediaType) = Library(
    id = id,
    name = id,
    displayOrder = 0,
    icon = Library.Icon.Database,
    mediaType = mediaType,
    provider = "",
    coverAspectRatio = 1,
    audiobooksOnly = false,
    createdAt = 0L,
    lastUpdate = 0L,
  )

  private inner class Libraries : LibraryRepository {
    override fun observeCurrentLibrary(refresh: Boolean): Flow<Library> = currentLibrary.filterNotNull()
    override fun observeAllLibraries(refresh: Boolean): Flow<List<Library>> = TODO()
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
    override suspend fun setCurrentLibrary(library: Library) = TODO()
    override suspend fun getAddPodcastContext(libraryId: LibraryId): Result<AddPodcastContext> = TODO()
  }

  private inner class Podcasts : PodcastsRepository {
    override suspend fun fetchEpisodeDownloads(libraryId: LibraryId): Result<EpisodeDownloadsSnapshot> {
      fetched += libraryId
      return Result.success(EpisodeDownloadsSnapshot(currentDownload = null, queue = emptyList()))
    }

    override fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode> = TODO()
    override suspend fun fetchPodcastFeedDetails(rssFeedUrl: String): Result<PodcastFeedDetails> = TODO()
    override suspend fun queueEpisodeDownloads(
      libraryItemId: LibraryItemId,
      episodes: List<RemotePodcastEpisode>,
    ): Result<Unit> = TODO()
    override suspend fun clearEpisodeDownloadQueue(libraryItemId: LibraryItemId): Result<Unit> = TODO()
    override suspend fun deleteEpisode(
      libraryItemId: LibraryItemId,
      episodeId: String,
      hardDelete: Boolean,
    ): Result<Unit> = TODO()
    override suspend fun searchPodcasts(query: String, country: String?): Result<List<PodcastSearchResult>> = TODO()
    override suspend fun addPodcast(
      libraryId: LibraryId,
      folder: LibraryFolder,
      draft: PodcastDraft,
      autoDownloadEpisodes: Boolean,
    ): Result<LibraryItemId> = TODO()
  }
}
