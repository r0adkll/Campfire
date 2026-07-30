// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.User
import app.campfire.data.SelectEpisodesWithLimitOffset
import app.campfire.db.paging.QueryPagingSource
import app.campfire.podcasts.api.LatestEpisode
import me.tatarka.inject.annotations.Inject

@Inject
class RecentEpisodesPagerFactory(
  private val remoteMediatorFactory: RecentEpisodesRemoteMediatorFactory,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
  private val urlHydrator: UrlHydrator,
) {

  @OptIn(ExperimentalPagingApi::class)
  fun create(user: User): Pager<Int, LatestEpisode> {
    return Pager(
      config = PagingConfig(
        pageSize = DEFAULT_PAGE_SIZE,
        initialLoadSize = DEFAULT_PAGE_SIZE,
        enablePlaceholders = false,
      ),
      remoteMediator = remoteMediatorFactory(user),
    ) {
      QueryPagingSource(
        countQuery = db.podcastEpisodePageQueries.count(
          userId = user.id,
          libraryId = user.selectedLibraryId,
        ),
        transacter = db.podcastEpisodePageQueries,
        context = dispatcherProvider.databaseRead,
        queryProvider = { limit: Long, offset: Long ->
          db.podcastEpisodePageQueries.selectEpisodesWithLimitOffset(
            userId = user.id,
            libraryId = user.selectedLibraryId,
            limit = limit,
            offset = offset,
          )
        },
        queryObserverProvider = { limit: Long, offset: Long ->
          db.podcastEpisodePageQueries.selectEpisodesPagesWithLimitOffset(
            userId = user.id,
            libraryId = user.selectedLibraryId,
            limit = limit,
            offset = offset,
          )
        },
        mapper = { row -> row.asLatestEpisode(urlHydrator, user) },
      )
    }
  }

  companion object {
    private const val DEFAULT_PAGE_SIZE = 50
  }
}

private fun SelectEpisodesWithLimitOffset.asLatestEpisode(
  urlHydrator: UrlHydrator,
  user: User,
): LatestEpisode {
  val contentUrl = audio_contentUrl
  val mimeType = audio_mimeType
  val duration = audio_duration
  val startOffset = audio_startOffset
  val codec = audio_codec
  val audioTitle = audio_title
  val trackIndex = audio_trackIndex
  val audioTrack = if (
    contentUrl != null && mimeType != null && duration != null &&
    startOffset != null && codec != null && audioTitle != null && trackIndex != null
  ) {
    AudioTrack(
      index = trackIndex,
      startOffset = startOffset.toFloat(),
      duration = duration.toFloat(),
      title = audioTitle,
      contentUrl = urlHydrator.hydrateUrl(contentUrl),
      mimeType = mimeType,
      codec = codec,
      // The audio_track join doesn't pull the FileMetadata; the bottom sheet/expanded
      // detail screen fetches the full track via getLibraryItem if needed. For the list
      // row we only need contentUrl + duration to start playback.
      metadata = FileMetadata(
        filename = "",
        ext = "",
        path = "",
        relPath = "",
        size = 0L,
        mtimeMs = 0L,
        ctimeMs = 0L,
        birthtimeMs = 0L,
      ),
      metaTags = null,
    )
  } else {
    null
  }

  val episode = PodcastEpisode(
    id = id,
    libraryItemId = libraryItemId,
    podcastId = podcastMediaId,
    index = episodeIndex,
    season = season,
    episode = episodeNumber,
    episodeType = episodeType,
    title = title,
    subtitle = subtitle,
    description = description,
    pubDate = pubDate,
    publishedAtMillis = publishedAtMillis,
    addedAtMillis = addedAtMillis,
    updatedAtMillis = updatedAtMillis,
    durationInMillis = durationInMillis,
    sizeInBytes = sizeInBytes,
    audioTrack = audioTrack,
    chapters = emptyList(),
  )

  val currentTime = progress_currentTime
  val progressFraction = progress_progress
  val isFinished = progress_isFinished
  val lastUpdate = progress_lastUpdate
  val progress = if (
    currentTime != null && progressFraction != null &&
    isFinished != null && lastUpdate != null
  ) {
    MediaProgress(
      id = "$libraryItemId-$id",
      userId = user.id,
      libraryItemId = libraryItemId,
      episodeId = id,
      mediaItemId = id,
      mediaItemType = MediaType.Podcast,
      duration = progress_duration?.toFloat() ?: durationInMillis.toFloat() / 1000f,
      progress = progressFraction.toFloat(),
      currentTime = currentTime.toFloat(),
      isFinished = isFinished,
      hideFromContinueListening = false,
      ebookLocation = null,
      ebookProgress = null,
      lastUpdate = lastUpdate,
      startedAt = lastUpdate,
      finishedAt = null,
      source = MediaProgress.Source.Local,
    )
  } else {
    null
  }

  return LatestEpisode(
    episode = episode,
    podcastTitle = podcast_title,
    podcastAuthor = podcast_author,
    coverImageUrl = urlHydrator.hydrateLibraryItem(libraryItemId),
    progress = progress,
  )
}
