// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui

import androidx.paging.Pager
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.User
import app.campfire.libraries.api.LibraryFolder
import app.campfire.podcasts.api.EpisodeDownloadsSnapshot
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastFeedDetails
import app.campfire.podcasts.api.PodcastSearchResult
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.RemotePodcastEpisode

class FakePodcastsRepository(
  var searchResult: Result<List<PodcastSearchResult>> = Result.success(emptyList()),
  var feedDetailsResult: Result<PodcastFeedDetails> = Result.failure(
    IllegalStateException("missing fake"),
  ),
  var addResult: Result<LibraryItemId> = Result.success("new_item"),
  var queueDownloadsResult: Result<Unit> = Result.success(Unit),
  var episodeDownloadsResult: Result<EpisodeDownloadsSnapshot> = Result.success(
    EpisodeDownloadsSnapshot(currentDownload = null, queue = emptyList()),
  ),
  var clearQueueResult: Result<Unit> = Result.success(Unit),
) : PodcastsRepository {

  data class SearchCall(val query: String, val country: String?)
  data class AddCall(
    val libraryId: LibraryId,
    val folder: LibraryFolder,
    val draft: PodcastDraft,
    val autoDownloadEpisodes: Boolean,
  )
  data class QueueDownloadsCall(
    val libraryItemId: LibraryItemId,
    val episodes: List<RemotePodcastEpisode>,
  )

  val searchCalls = mutableListOf<SearchCall>()
  val feedCalls = mutableListOf<String>()
  val addCalls = mutableListOf<AddCall>()
  val queueDownloadsCalls = mutableListOf<QueueDownloadsCall>()
  val fetchEpisodeDownloadsCalls = mutableListOf<LibraryId>()
  val clearQueueCalls = mutableListOf<LibraryItemId>()

  override fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode> {
    throw NotImplementedError("not exercised in these tests")
  }

  override suspend fun fetchPodcastFeedDetails(
    rssFeedUrl: String,
  ): Result<PodcastFeedDetails> {
    feedCalls += rssFeedUrl
    return feedDetailsResult
  }

  override suspend fun queueEpisodeDownloads(
    libraryItemId: LibraryItemId,
    episodes: List<RemotePodcastEpisode>,
  ): Result<Unit> {
    queueDownloadsCalls += QueueDownloadsCall(libraryItemId, episodes)
    return queueDownloadsResult
  }

  override suspend fun fetchEpisodeDownloads(libraryId: LibraryId): Result<EpisodeDownloadsSnapshot> {
    fetchEpisodeDownloadsCalls += libraryId
    return episodeDownloadsResult
  }

  override suspend fun clearEpisodeDownloadQueue(libraryItemId: LibraryItemId): Result<Unit> {
    clearQueueCalls += libraryItemId
    return clearQueueResult
  }

  override suspend fun searchPodcasts(
    query: String,
    country: String?,
  ): Result<List<PodcastSearchResult>> {
    searchCalls += SearchCall(query, country)
    return searchResult
  }

  override suspend fun addPodcast(
    libraryId: LibraryId,
    folder: LibraryFolder,
    draft: PodcastDraft,
    autoDownloadEpisodes: Boolean,
  ): Result<LibraryItemId> {
    addCalls += AddCall(libraryId, folder, draft, autoDownloadEpisodes)
    return addResult
  }
}
