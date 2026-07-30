// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.MediaType
import app.campfire.data.LibraryItem as DbLibraryItem
import app.campfire.data.PodcastEpisode as DbPodcastEpisode
import app.campfire.data.PodcastEpisodeAudioTrack as DbPodcastEpisodeAudioTrack
import app.campfire.data.PodcastMedia as DbPodcastMedia
import app.campfire.network.models.RecentPodcastEpisode
import kotlin.time.Duration.Companion.seconds

// ===================================================================================
// Network → DB
// ===================================================================================

/**
 * Build a stub [DbLibraryItem] row sufficient to satisfy the foreign key from podcastMedia /
 * podcastEpisode → libraryItem. The recent-episodes endpoint does not return the libraryItem-
 * level fields (ino, folderId, path, etc.), so the stub uses safe placeholders. Call sites must
 * `INSERT OR IGNORE` so a real row from the libraries flow is never overwritten.
 */
fun RecentPodcastEpisode.asLibraryItemStubDbModel(serverUrl: String): DbLibraryItem {
  return DbLibraryItem(
    id = libraryItemId,
    ino = "",
    libraryId = libraryId,
    oldLibraryItemId = null,
    folderId = "",
    path = "",
    relPath = "",
    isFile = false,
    mtimeMs = addedAt,
    ctimeMs = addedAt,
    birthtimeMs = addedAt,
    addedAt = addedAt,
    updatedAt = updatedAt,
    isMissing = false,
    isInvalid = false,
    mediaType = MediaType.Podcast,
    numFiles = 0,
    size = sizeBytes,
    serverUrl = serverUrl,
  )
}

fun RecentPodcastEpisode.asEpisodeDbModel(): DbPodcastEpisode {
  val resolvedDuration = duration.takeIf { it > 0.0 }
    ?: audioFile?.duration?.toDouble()
    ?: audioTrack?.duration?.toDouble()
    ?: 0.0
  val durationMillis = resolvedDuration.seconds.inWholeMilliseconds
  val sizeBytes = sizeBytes.takeIf { it > 0L } ?: audioFile?.metadata?.size ?: 0L

  return DbPodcastEpisode(
    id = id,
    libraryItemId = libraryItemId,
    podcastMediaId = podcast.id,
    episodeIndex = index,
    season = season,
    episodeNumber = episode,
    episodeType = episodeType,
    title = title,
    subtitle = subtitle,
    description = description,
    pubDate = pubDate,
    publishedAtMillis = publishedAt,
    addedAtMillis = addedAt,
    updatedAtMillis = updatedAt,
    durationInMillis = durationMillis,
    sizeInBytes = sizeBytes,
  )
}

/**
 * Map the parent [Podcast][app.campfire.network.models.Podcast] context returned alongside each
 * recent episode into a minimal [DbPodcastMedia] row. The recent-episodes endpoint does not
 * include the parent libraryItem-level fields, so [libraryItemId] is supplied by the caller from
 * the episode itself.
 */
fun RecentPodcastEpisode.asPodcastMediaDbModel(
  urlHydrator: UrlHydrator,
): DbPodcastMedia = podcast.asDbModel(libraryItemId, urlHydrator)

/**
 * Map the parent [audioTrack] into a [DbPodcastEpisodeAudioTrack] row, or null if the server
 * didn't return one (rare, but possible for broken/in-progress episodes).
 */
fun RecentPodcastEpisode.asEpisodeAudioTrackDbModelOrNull(): DbPodcastEpisodeAudioTrack? {
  return audioTrack?.asEpisodeDbModel(id)
}
