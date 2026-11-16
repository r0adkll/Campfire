// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts

import androidx.paging.Pager
import app.campfire.CampfireDatabase
import app.campfire.audioplayer.PlaybackController
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.User
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.api.LibraryFolder
import app.campfire.network.ApiException
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.isForbidden
import app.campfire.network.isNotFound
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloads
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
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultPodcastsRepository(
  private val pagerFactory: RecentEpisodesPagerFactory,
  private val api: AudioBookShelfApi,
  private val downloadSink: RemoteEpisodeDownloadSink,
  private val db: CampfireDatabase,
  private val sessionQueue: SessionQueue,
  private val sessionsRepository: SessionsRepository,
  private val playbackController: PlaybackController,
  private val downloads: Downloads,
  private val dispatcherProvider: DispatcherProvider,
) : PodcastsRepository {

  companion object : Corked("PodcastsRepository")

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

  override suspend fun deleteEpisode(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId,
    hardDelete: Boolean,
  ): Result<Unit> {
    val error = api.deletePodcastEpisode(libraryItemId, episodeId, hardDelete).exceptionOrNull()
    when {
      error == null -> Unit
      // Already gone, deleted from somewhere else, which is all that was asked
      error.isNotFound -> ibark { "Episode $episodeId was already deleted from the server" }
      error.isForbidden -> return Result.failure(ItemChangeNotAllowedException(error))
      else -> return Result.failure(error)
    }

    // The server has let go of it, so what's left is only tidying up here: a part of it that
    // fails doesn't undo the delete
    try {
      forgetEpisode(libraryItemId, episodeId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      ebark(e) { "Failed to forget deleted episode $episodeId" }
    }
    return Result.success(Unit)
  }

  private suspend fun forgetEpisode(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.transaction {
        // Grouped statements return a lazy result that only runs once awaited
        db.podcastEpisodeQueries.purgeForId(episodeId).await()
      }
    }
    sessionQueue.remove(libraryItemId, episodeId)
    downloads.delete(DownloadContent.Episode(libraryItemId, episodeId))

    // Its listening session can't go on with an episode the server no longer has. Stopping only
    // when it's the one playing, as stopping ends whatever plays; out of the queue first, so the
    // next one up isn't this episode
    if (sessionsRepository.observePlayingItem().first()?.matches(libraryItemId, episodeId) == true) {
      playbackController.stopSession(itemId = libraryItemId, episodeId = episodeId)
    }
    sessionsRepository.markDeleted(libraryItemId, episodeId)
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
