// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.data.LibraryItem
import app.campfire.data.LibraryItemsQueries
import app.campfire.data.PodcastMedia
import app.campfire.data.PodcastMediaQueries

/**
 * Inserts [row], or refreshes the existing row in place. List and shelf writes use this so newer
 * server data replaces what an earlier, partial write stored, without the cascading delete that
 * `insert` (INSERT OR REPLACE) causes.
 */
suspend fun LibraryItemsQueries.upsert(row: LibraryItem) {
  upsert(
    id = row.id,
    ino = row.ino,
    libraryId = row.libraryId,
    oldLibraryItemId = row.oldLibraryItemId,
    folderId = row.folderId,
    path = row.path,
    relPath = row.relPath,
    isFile = row.isFile,
    mtimeMs = row.mtimeMs,
    ctimeMs = row.ctimeMs,
    birthtimeMs = row.birthtimeMs,
    addedAt = row.addedAt,
    updatedAt = row.updatedAt,
    isMissing = row.isMissing,
    isInvalid = row.isInvalid,
    mediaType = row.mediaType,
    numFiles = row.numFiles,
    size = row.size,
    serverUrl = row.serverUrl,
  ).await()
}

/**
 * Inserts [row], or refreshes the existing row in place. `insert` would delete the podcast's
 * episodes through their foreign key.
 */
suspend fun PodcastMediaQueries.upsert(row: PodcastMedia) {
  upsert(
    mediaId = row.mediaId,
    libraryItemId = row.libraryItemId,
    coverPath = row.coverPath,
    tags = row.tags,
    sizeInBytes = row.sizeInBytes,
    numEpisodes = row.numEpisodes,
    autoDownloadEpisodes = row.autoDownloadEpisodes,
    autoDownloadSchedule = row.autoDownloadSchedule,
    lastEpisodeCheckMillis = row.lastEpisodeCheckMillis,
    maxEpisodesToKeep = row.maxEpisodesToKeep,
    maxNewEpisodesToDownload = row.maxNewEpisodesToDownload,
    latestEpisodePublishedAtMillis = row.latestEpisodePublishedAtMillis,
    metadata_title = row.metadata_title,
    metadata_titleIgnorePrefix = row.metadata_titleIgnorePrefix,
    metadata_author = row.metadata_author,
    metadata_description = row.metadata_description,
    metadata_releaseDate = row.metadata_releaseDate,
    metadata_genres = row.metadata_genres,
    metadata_feedUrl = row.metadata_feedUrl,
    metadata_imageUrl = row.metadata_imageUrl,
    metadata_itunesPageUrl = row.metadata_itunesPageUrl,
    metadata_itunesId = row.metadata_itunesId,
    metadata_itunesArtistId = row.metadata_itunesArtistId,
    metadata_explicit = row.metadata_explicit,
    metadata_language = row.metadata_language,
    metadata_podcastType = row.metadata_podcastType,
  ).await()
}
