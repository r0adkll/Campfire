// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.podcast

import androidx.paging.Pager
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.User
import app.campfire.libraries.api.LibraryFolder
import app.campfire.podcasts.api.EpisodeDownloadsSnapshot
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastFeedDetails
import app.campfire.podcasts.api.PodcastSearchResult
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.RemotePodcastEpisode

/** Records episode deletes, the only part of the podcasts repository a podcast's screen uses. */
class FakeEpisodeDeletingPodcastsRepository : PodcastsRepository {

  data class DeleteEpisodeCall(
    val libraryItemId: LibraryItemId,
    val episodeId: PodcastEpisodeId,
    val hardDelete: Boolean,
  )

  var deleteEpisodeResult: Result<Unit> = Result.success(Unit)
  val deleteEpisodeCalls = mutableListOf<DeleteEpisodeCall>()

  override suspend fun deleteEpisode(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId,
    hardDelete: Boolean,
  ): Result<Unit> {
    deleteEpisodeCalls += DeleteEpisodeCall(libraryItemId, episodeId, hardDelete)
    return deleteEpisodeResult
  }

  override fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode> = TODO()
  override suspend fun fetchPodcastFeedDetails(rssFeedUrl: String): Result<PodcastFeedDetails> = TODO()
  override suspend fun queueEpisodeDownloads(
    libraryItemId: LibraryItemId,
    episodes: List<RemotePodcastEpisode>,
  ): Result<Unit> = TODO()
  override suspend fun fetchEpisodeDownloads(libraryId: LibraryId): Result<EpisodeDownloadsSnapshot> = TODO()
  override suspend fun clearEpisodeDownloadQueue(libraryItemId: LibraryItemId): Result<Unit> = TODO()
  override suspend fun searchPodcasts(query: String, country: String?): Result<List<PodcastSearchResult>> = TODO()
  override suspend fun addPodcast(
    libraryId: LibraryId,
    folder: LibraryFolder,
    draft: PodcastDraft,
    autoDownloadEpisodes: Boolean,
  ): Result<LibraryItemId> = TODO()
}
