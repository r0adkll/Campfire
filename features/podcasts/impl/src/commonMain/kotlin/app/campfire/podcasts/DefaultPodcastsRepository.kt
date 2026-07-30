// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts

import androidx.paging.Pager
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.User
import app.campfire.libraries.api.LibraryFolder
import app.campfire.network.ApiException
import app.campfire.network.AudioBookShelfApi
import app.campfire.podcasts.api.AddPodcastException
import app.campfire.podcasts.api.EpisodeDownloadsSnapshot
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.PodcastDraft
import app.campfire.podcasts.api.PodcastFeedDetails
import app.campfire.podcasts.api.PodcastSearchResult
import app.campfire.podcasts.api.PodcastsRepository
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.RemotePodcastEpisode
import app.campfire.podcasts.api.sanitizePodcastPathSegment
import app.campfire.podcasts.downloads.RemoteEpisodeDownloadSink
import app.campfire.podcasts.downloads.toDomain
import app.campfire.podcasts.mapping.asCreateMetadata
import app.campfire.podcasts.mapping.asDomainModel
import app.campfire.podcasts.mapping.asDomainModelOrNull
import app.campfire.podcasts.mapping.asDraftOrNull
import app.campfire.podcasts.mapping.asNetworkModel
import app.campfire.podcasts.paging.RecentEpisodesPagerFactory
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultPodcastsRepository(
  private val pagerFactory: RecentEpisodesPagerFactory,
  private val api: AudioBookShelfApi,
  private val downloadSink: RemoteEpisodeDownloadSink,
) : PodcastsRepository {

  override fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode> {
    return pagerFactory.create(user)
  }

  override suspend fun fetchPodcastFeedDetails(
    rssFeedUrl: String,
  ): Result<PodcastFeedDetails> {
    return api.getPodcastFeed(rssFeedUrl).mapCatching { feed ->
      val draft = feed.asDraftOrNull(rssFeedUrl)
        ?: error("Podcast feed at $rssFeedUrl has no usable metadata")
      val episodes = feed.episodes
        .filter { it.enclosure.url.isNotBlank() }
        .map { it.asDomainModel() }
      PodcastFeedDetails(draft = draft, episodes = episodes)
    }
  }

  override suspend fun queueEpisodeDownloads(
    libraryItemId: LibraryItemId,
    episodes: List<RemotePodcastEpisode>,
  ): Result<Unit> {
    if (episodes.isEmpty()) return Result.success(Unit)
    return api.downloadPodcastEpisodes(
      libraryItemId = libraryItemId,
      episodes = episodes.map { it.asNetworkModel() },
    )
  }

  override suspend fun fetchEpisodeDownloads(libraryId: LibraryId): Result<EpisodeDownloadsSnapshot> {
    return api.getEpisodeDownloads(libraryId).map { response ->
      val snapshot = EpisodeDownloadsSnapshot(
        currentDownload = response.currentDownload?.toDomain(RemoteEpisodeDownload.State.Downloading),
        queue = response.queue.map { it.toDomain(RemoteEpisodeDownload.State.Queued) },
      )
      downloadSink.applySnapshot(libraryId, snapshot)
      snapshot
    }
  }

  override suspend fun clearEpisodeDownloadQueue(libraryItemId: LibraryItemId): Result<Unit> {
    return api.clearPodcastDownloadQueue(libraryItemId)
  }

  override suspend fun searchPodcasts(
    query: String,
    country: String?,
  ): Result<List<PodcastSearchResult>> {
    return api.searchPodcasts(term = query, country = country)
      .map { results -> results.mapNotNull { it.asDomainModelOrNull() } }
  }

  override suspend fun addPodcast(
    libraryId: LibraryId,
    folder: LibraryFolder,
    draft: PodcastDraft,
    autoDownloadEpisodes: Boolean,
  ): Result<LibraryItemId> {
    val pathSegment = sanitizePodcastPathSegment(draft.title)
    val path = "${folder.fullPath.trimEnd('/')}/$pathSegment"
    return api.createPodcast(
      libraryId = libraryId,
      folderId = folder.id,
      path = path,
      metadata = draft.asCreateMetadata(),
      tags = emptyList(),
      autoDownloadEpisodes = autoDownloadEpisodes,
      autoDownloadSchedule = null,
    ).fold(
      onSuccess = { Result.success(it.id) },
      onFailure = { error ->
        val kind = when ((error as? ApiException)?.statusCode) {
          403 -> AddPodcastException.Kind.Forbidden
          400 -> AddPodcastException.Kind.PathConflict
          else -> AddPodcastException.Kind.Generic
        }
        Result.failure(AddPodcastException(kind))
      },
    )
  }
}
