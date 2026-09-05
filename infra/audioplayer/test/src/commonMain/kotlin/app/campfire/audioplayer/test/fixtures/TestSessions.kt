// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.test.fixtures

import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

fun track(index: Int, startOffset: Float, duration: Float) = AudioTrack(
  index = index,
  startOffset = startOffset,
  duration = duration,
  title = "Track $index",
  contentUrl = "/track$index.m4b",
  mimeType = "audio/mp4",
  codec = "aac",
  metadata = FileMetadata("track$index.m4b", ".m4b", "/track$index.m4b", "track$index.m4b", 0, 0, 0, 0),
  metaTags = null,
)

fun chapter(id: Int, start: Float, end: Float) = Chapter(
  id = id,
  start = start,
  end = end,
  title = "Chapter ${id + 1}",
)

/** A book session over [tracks] with [chapters], resumed at [currentTime]. */
fun session(
  chapters: List<Chapter> = emptyList(),
  tracks: List<AudioTrack> = emptyList(),
  currentTime: Duration = 0.seconds,
  hlsStreamUrl: String? = null,
): Session {
  val media = Media.Book(
    id = "media-1",
    metadata = Media.Metadata.Book(
      title = "A Book",
      titleIgnorePrefix = "A Book",
      subtitle = null,
      authorName = "An Author",
      authorNameLastFirst = "Author, An",
      narratorName = null,
      seriesName = null,
      series = emptyList(),
      genres = emptyList(),
      publishedYear = null,
      publishedDate = null,
      publisher = null,
      description = null,
      ISBN = null,
      ASIN = null,
      language = null,
      isExplicit = false,
      isAbridged = false,
    ),
    coverImageUrl = "/cover.jpg",
    coverPath = null,
    tags = emptyList(),
    numTracks = tracks.size,
    numAudioFiles = tracks.size,
    numChapters = chapters.size,
    numMissingParts = 0,
    numInvalidAudioFiles = 0,
    durationInMillis = (tracks.sumOf { it.duration.toDouble() } * 1000).toLong(),
    sizeInBytes = 0L,
    chapters = chapters,
    tracks = tracks,
  )
  val item = LibraryItem(
    id = "item-1",
    ino = "ino-1",
    libraryId = "lib-1",
    folderId = "folder-1",
    path = "/audiobooks/test",
    relPath = "test",
    isFile = false,
    mtimeMs = 0L,
    ctimeMs = 0L,
    birthtimeMs = 0L,
    isMissing = false,
    isInvalid = false,
    mediaType = MediaType.Book,
    numFiles = 1,
    sizeInBytes = 0L,
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    media = media,
  )
  val now = LocalDateTime(2026, 1, 1, 0, 0)
  return Session(
    id = Uuid.random(),
    libraryItem = item,
    userId = "user-1",
    isDeleted = false,
    playMethod = PlayMethod.DirectPlay,
    mediaPlayer = "campfire",
    timeListening = 0.seconds,
    startTime = 0.seconds,
    currentTime = currentTime,
    lastPlayedAt = null,
    startedAt = now,
    updatedAt = now,
    hlsStreamUrl = hlsStreamUrl,
  )
}
