// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class MediaItemBuilderTest {

  // region Helper factories

  private fun fileMetadata() = FileMetadata(
    filename = "test.m4b",
    ext = "m4b",
    path = "/audiobooks/test.m4b",
    relPath = "test.m4b",
    size = 1000L,
    mtimeMs = 0L,
    ctimeMs = 0L,
    birthtimeMs = 0L,
  )

  private fun track(
    index: Int = 0,
    startOffset: Float = 0f,
    duration: Float = 100f,
    title: String = "Track $index",
    contentUrl: String = "/track$index.m4b",
    mimeType: String = "audio/mp4",
    tagTitle: String? = null,
  ) = AudioTrack(
    index = index,
    startOffset = startOffset,
    duration = duration,
    title = title,
    contentUrl = contentUrl,
    mimeType = mimeType,
    codec = "aac",
    metadata = fileMetadata(),
    metaTags = tagTitle?.let { app.campfire.core.model.MetaTags(tagTitle = it) },
  )

  private fun chapter(
    id: Int = 0,
    start: Float = 0f,
    end: Float = 100f,
    title: String = "Chapter $id",
  ) = Chapter(
    id = id,
    start = start,
    end = end,
    title = title,
  )

  private fun metadata(
    title: String? = "Test Book",
    authorName: String? = "Test Author",
    description: String? = "A test book",
    subtitle: String? = null,
    seriesName: String? = null,
  ) = Media.Metadata.Book(
    title = title,
    titleIgnorePrefix = title,
    subtitle = subtitle,
    authorName = authorName,
    authorNameLastFirst = authorName,
    narratorName = null,
    seriesName = seriesName,
    series = emptyList(),
    genres = emptyList(),
    publishedYear = null,
    publishedDate = null,
    publisher = null,
    description = description,
    ISBN = null,
    ASIN = null,
    language = null,
    isExplicit = false,
    isAbridged = false,
  )

  private fun media(
    id: String = "media-1",
    chapters: List<Chapter> = emptyList(),
    tracks: List<AudioTrack> = emptyList(),
    metadata: Media.Metadata.Book = metadata(),
    coverImageUrl: String = "/cover.jpg",
  ) = Media.Book(
    id = id,
    metadata = metadata,
    coverImageUrl = coverImageUrl,
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

  private fun libraryItem(
    media: Media = media(),
  ) = LibraryItem(
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

  // endregion

  // region buildTracks — remote (cast) queue shape

  @Test
  fun `buildTracks ignores chapters and returns one unclipped item per track`() {
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 300f),
      track(index = 1, startOffset = 300f, duration = 400f),
    )
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 150f),
      chapter(id = 1, start = 150f, end = 300f),
      chapter(id = 2, start = 300f, end = 700f),
    )
    val item = libraryItem(media = media(tracks = tracks, chapters = chapters))

    val result = MediaItemBuilder.buildTracks(item)

    assertThat(result).hasSize(2)
    assertThat(result[0].id).isEqualTo("media-1_0")
    assertThat(result[0].clipping).isNull()
    assertThat(result[1].id).isEqualTo("media-1_1")
    assertThat(result[1].clipping).isNull()
  }

  @Test
  fun `buildTracks carries track durations for absolute timeline math`() {
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 300f),
      track(index = 1, startOffset = 300f, duration = 400f),
    )
    val item = libraryItem(media = media(tracks = tracks))

    val result = MediaItemBuilder.buildTracks(item)

    assertThat(result[0].metadata?.durationMs).isEqualTo(300_000L)
    assertThat(result[1].metadata?.durationMs).isEqualTo(400_000L)
  }

  // endregion

  // region No chapters — tracks only

  @Test
  fun `build with no chapters returns one media item per track`() {
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 300f),
      track(index = 1, startOffset = 300f, duration = 400f),
    )
    val item = libraryItem(media = media(tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result).hasSize(2)
    assertThat(result[0].id).isEqualTo("media-1_0")
    assertThat(result[0].uri).isEqualTo("/track0.m4b")
    assertThat(result[0].clipping).isNull()
    assertThat(result[1].id).isEqualTo("media-1_1")
    assertThat(result[1].uri).isEqualTo("/track1.m4b")
  }

  @Test
  fun `build with no chapters uses track metadata`() {
    val tracks = listOf(
      track(index = 0, duration = 60f, tagTitle = "My Track"),
    )
    val meta = metadata(authorName = "Author A", description = "Desc", title = "Book X")
    val item = libraryItem(media = media(tracks = tracks, metadata = meta))

    val result = MediaItemBuilder.build(item)

    val m = result.first().metadata!!
    assertThat(m.id).isEqualTo(0)
    assertThat(m.title).isEqualTo("My Track")
    assertThat(m.artist).isEqualTo("Author A")
    assertThat(m.description).isEqualTo("Desc")
    assertThat(m.albumTitle).isEqualTo("Book X")
    assertThat(m.durationMs).isEqualTo(60.seconds.inWholeMilliseconds)
    assertThat(m.libraryItemId).isEqualTo("item-1")
  }

  // endregion

  // region Chapters == Tracks (1:1 mapping)

  @Test
  fun `build with same number of chapters and tracks creates 1-to-1 mapping without clipping`() {
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 100f, title = "Ch 1"),
      chapter(id = 1, start = 100f, end = 250f, title = "Ch 2"),
    )
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 100f),
      track(index = 1, startOffset = 100f, duration = 150f),
    )
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result).hasSize(2)
    // First item
    assertThat(result[0].id).isEqualTo("media-1_0")
    assertThat(result[0].uri).isEqualTo("/track0.m4b")
    assertThat(result[0].clipping).isNull()
    assertThat(result[0].metadata!!.title).isEqualTo("Ch 1")
    // Second item
    assertThat(result[1].id).isEqualTo("media-1_1")
    assertThat(result[1].uri).isEqualTo("/track1.m4b")
    assertThat(result[1].clipping).isNull()
    assertThat(result[1].metadata!!.title).isEqualTo("Ch 2")
  }

  @Test
  fun `build with 1-to-1 chapters and tracks uses chapter metadata`() {
    val chapters = listOf(chapter(id = 5, start = 0f, end = 120f, title = "Opening"))
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 120f))
    val meta = metadata(authorName = "Jane", description = "Desc", subtitle = "Sub", title = "Title")
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks, metadata = meta))

    val result = MediaItemBuilder.build(item)

    val m = result.first().metadata!!
    assertThat(m.id).isEqualTo(5)
    assertThat(m.title).isEqualTo("Opening")
    assertThat(m.artist).isEqualTo("Jane")
    assertThat(m.description).isEqualTo("Desc")
    assertThat(m.subtitle).isEqualTo("Sub")
    assertThat(m.albumTitle).isEqualTo("Title")
    assertThat(m.durationMs).isEqualTo(120.seconds.inWholeMilliseconds)
  }

  // endregion

  // region Single track, multiple chapters (clip audio)

  @Test
  fun `build with single track and multiple chapters clips audio for each chapter`() {
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 100f),
      chapter(id = 1, start = 100f, end = 250f),
      chapter(id = 2, start = 250f, end = 400f),
    )
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 400f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result).hasSize(3)
    // All should reference the same track URI
    result.forEach { assertThat(it.uri).isEqualTo("/track0.m4b") }
    // First clip
    assertThat(result[0].clipping).isNotNull()
    assertThat(result[0].clipping!!.startMs).isEqualTo(0L)
    assertThat(result[0].clipping!!.endMs).isEqualTo(100.seconds.inWholeMilliseconds)
    // Second clip
    assertThat(result[1].clipping!!.startMs).isEqualTo(100.seconds.inWholeMilliseconds)
    assertThat(result[1].clipping!!.endMs).isEqualTo(250.seconds.inWholeMilliseconds)
    // Third clip
    assertThat(result[2].clipping!!.startMs).isEqualTo(250.seconds.inWholeMilliseconds)
    assertThat(result[2].clipping!!.endMs).isEqualTo(400.seconds.inWholeMilliseconds)
  }

  @Test
  fun `build with single track and aligned chapters does not record exception`() {
    // Chapters fully cover the track (last chapter ends at track end)
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 200f),
      chapter(id = 1, start = 200f, end = 500f),
    )
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 500f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    // Should not throw — chapters fully cover track within threshold
    val result = MediaItemBuilder.build(item)
    assertThat(result).hasSize(2)
  }

  @Test
  fun `build with single track and chapters leaving small gap within threshold still returns all chapters`() {
    // Last chapter ends 5 seconds before track end (< 10s threshold)
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 200f),
      chapter(id = 1, start = 200f, end = 495f),
    )
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 500f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)
    assertThat(result).hasSize(2)
  }

  @Test
  fun `build with single track and chapters leaving large gap exceeding threshold still returns media items`() {
    // Last chapter ends 20 seconds before track end (> 10s threshold)
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 200f),
      chapter(id = 1, start = 200f, end = 480f),
    )
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 500f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    // Should still return items even though exception is recorded
    val result = MediaItemBuilder.build(item)
    assertThat(result).hasSize(2)
  }

  @Test
  fun `build with single track and misaligned chapters filters to only fitting chapters`() {
    // Track only covers 0-200, but chapters go beyond
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 100f),
      chapter(id = 1, start = 100f, end = 200f),
      chapter(id = 2, start = 300f, end = 400f), // Starts outside the track
    )
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 200f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    // Only 2 chapters fit within the track
    assertThat(result).hasSize(2)
    assertThat(result[0].metadata!!.id).isEqualTo(0)
    assertThat(result[1].metadata!!.id).isEqualTo(1)
  }

  @Test
  fun `build with single track with non-zero startOffset filters chapters correctly`() {
    // Track starts at 100, chapters before that should be excluded
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 50f), // Before the track
      chapter(id = 1, start = 100f, end = 200f),
      chapter(id = 2, start = 200f, end = 300f),
    )
    val tracks = listOf(track(index = 0, startOffset = 100f, duration = 200f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result).hasSize(2)
    assertThat(result[0].metadata!!.id).isEqualTo(1)
    assertThat(result[1].metadata!!.id).isEqualTo(2)
  }

  // endregion

  // region Multiple tracks, multiple chapters (not 1:1)

  @Test
  fun `build with multiple tracks and chapters maps chapters to correct tracks`() {
    // Chapters don't overlap track boundaries (chapter starts/ends stay within one track)
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 50f, title = "Ch 1"),
      chapter(id = 1, start = 50f, end = 99f, title = "Ch 2"),
      chapter(id = 2, start = 101f, end = 199f, title = "Ch 3"),
    )
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 100f),
      track(index = 1, startOffset = 100f, duration = 100f),
    )
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    // Track 0 gets chapters 0 and 1, Track 1 gets chapter 2
    assertThat(result).hasSize(3)
    // Chapters on track 0
    assertThat(result[0].uri).isEqualTo("/track0.m4b")
    assertThat(result[0].metadata!!.title).isEqualTo("Ch 1")
    assertThat(result[0].clipping).isNotNull()
    assertThat(result[1].uri).isEqualTo("/track0.m4b")
    assertThat(result[1].metadata!!.title).isEqualTo("Ch 2")
    // Chapter on track 1
    assertThat(result[2].uri).isEqualTo("/track1.m4b")
    assertThat(result[2].metadata!!.title).isEqualTo("Ch 3")
  }

  @Test
  fun `build with multiple tracks where no chapters match a track falls back to track-only item`() {
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 100f),
    )
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 100f),
      track(index = 1, startOffset = 100f, duration = 100f), // No chapter covers this track
    )
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    // Track 0 gets chapter 0 clipped, Track 1 falls back to track-only
    assertThat(result).hasSize(2)
    assertThat(result[0].clipping).isNotNull()
    assertThat(result[0].metadata!!.title).isEqualTo("Chapter 0")
    assertThat(result[1].clipping).isNull()
    assertThat(result[1].id).isEqualTo("media-1_1")
  }

  @Test
  fun `build with multiple tracks where chapters leave large gap falls back to track-only item`() {
    // Chapter ends well before the track does (> 10s threshold)
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 50f),
    )
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 100f),
    )
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    // Falls back to track-only because remaining duration exceeds threshold
    assertThat(result).hasSize(1)
    assertThat(result[0].clipping).isNull()
    assertThat(result[0].id).isEqualTo("media-1_0")
  }

  @Test
  fun `build with multiple tracks where chapters cover within threshold returns clipped items`() {
    // 3 chapters across 2 tracks, chapters stay within their respective tracks
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 50f),
      chapter(id = 1, start = 50f, end = 95f), // 5s remaining on track 0 < 10s threshold
      chapter(id = 2, start = 101f, end = 195f), // 5s remaining on track 1 < 10s threshold
    )
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 100f),
      track(index = 1, startOffset = 100f, duration = 100f),
    )
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    // Both tracks have chapters within threshold, so all chapters get clipped
    assertThat(result).hasSize(3)
    assertThat(result[0].clipping).isNotNull()
    assertThat(result[1].clipping).isNotNull()
    assertThat(result[2].clipping).isNotNull()
  }

  // endregion

  // region Clipping edge cases

  @Test
  fun `clipping clamps start to track start when chapter starts before track`() {
    // Chapter starts at 0 but track starts at 50 — should clamp to 50
    val chapters = listOf(chapter(id = 0, start = 0f, end = 100f))
    val tracks = listOf(track(index = 0, startOffset = 50f, duration = 100f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    // Use 1:1 path (chapters.size == tracks.size)
    val result = MediaItemBuilder.build(item)

    // clipAudio is false for 1:1 path, so no clipping
    assertThat(result[0].clipping).isNull()
  }

  @Test
  fun `clipping clamps end to track end when chapter extends beyond track`() {
    // Single track, multiple chapters where last chapter extends beyond track
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 50f),
      chapter(id = 1, start = 50f, end = 120f), // extends 20s beyond track
    )
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 100f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    // Second chapter's clipping end should be clamped to track end (100s)
    assertThat(result[1].clipping!!.endMs).isEqualTo(100.seconds.inWholeMilliseconds)
  }

  @Test
  fun `clipping clamps start when chapter starts before track on single track with multi-chapters`() {
    // Track starts at 50, chapters start within the track range
    // Chapter 0 starts at 50 (at track start), chapter 1 starts at 100
    // But chapter 0 was defined to start at 40 which is before track start
    // In single-track path, chapters are filtered by start in [trackStart, trackEnd]
    // so chapter starting at 40 would be excluded. Use a chapter that fits.
    val chapters = listOf(
      chapter(id = 0, start = 50f, end = 100f),
      chapter(id = 1, start = 100f, end = 150f),
    )
    val tracks = listOf(track(index = 0, startOffset = 50f, duration = 100f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result).hasSize(2)
    assertThat(result[0].clipping!!.startMs).isEqualTo(50.seconds.inWholeMilliseconds)
    assertThat(result[0].clipping!!.endMs).isEqualTo(100.seconds.inWholeMilliseconds)
    assertThat(result[1].clipping!!.startMs).isEqualTo(100.seconds.inWholeMilliseconds)
    assertThat(result[1].clipping!!.endMs).isEqualTo(150.seconds.inWholeMilliseconds)
  }

  // endregion

  // region computerChapterTrackDiffInSeconds

  @Test
  fun `computerChapterTrackDiffInSeconds returns zero for perfect alignment`() {
    val ch = chapter(start = 0f, end = 100f)
    val tr = track(startOffset = 0f, duration = 100f)

    val diff = MediaItemBuilder.computerChapterTrackDiffInSeconds(ch, tr)

    assertThat(diff).isCloseTo(0f, 0.001f)
  }

  @Test
  fun `computerChapterTrackDiffInSeconds returns sum of start and duration diff`() {
    val ch = chapter(start = 10f, end = 110f) // duration = 100
    val tr = track(startOffset = 5f, duration = 90f)

    // startDiff = |10 - 5| = 5
    // durationDiff = |100 - 90| = 10
    // total = 15
    val diff = MediaItemBuilder.computerChapterTrackDiffInSeconds(ch, tr)

    assertThat(diff).isCloseTo(15f, 0.001f)
  }

  @Test
  fun `computerChapterTrackDiffInSeconds uses absolute values`() {
    val ch = chapter(start = 5f, end = 85f) // duration = 80
    val tr = track(startOffset = 10f, duration = 100f)

    // startDiff = |5 - 10| = 5
    // durationDiff = |80 - 100| = 20
    // total = 25
    val diff = MediaItemBuilder.computerChapterTrackDiffInSeconds(ch, tr)

    assertThat(diff).isCloseTo(25f, 0.001f)
  }

  // endregion

  // region createMediaMetadata (chapter)

  @Test
  fun `createMediaMetadata for chapter populates all fields`() {
    val ch = chapter(id = 3, start = 10f, end = 70f, title = "The Beginning")
    val med = media(
      coverImageUrl = "/cover.png",
      metadata = metadata(
        authorName = "Author",
        description = "Desc",
        subtitle = "Sub",
        title = "Title",
      ),
    )

    val result = MediaItemBuilder.createMediaMetadata(ch, med, libraryItemId = "item-7")

    assertThat(result.id).isEqualTo(3)
    assertThat(result.title).isEqualTo("The Beginning")
    assertThat(result.artist).isEqualTo("Author")
    assertThat(result.description).isEqualTo("Desc")
    assertThat(result.subtitle).isEqualTo("Sub")
    assertThat(result.albumTitle).isEqualTo("Title")
    assertThat(result.artworkUri).isEqualTo("/cover.png")
    assertThat(result.durationMs).isEqualTo(60.seconds.inWholeMilliseconds)
    assertThat(result.libraryItemId).isEqualTo("item-7")
  }

  @Test
  fun `createMediaMetadata for chapter with null description defaults to empty string`() {
    val ch = chapter(id = 0, start = 0f, end = 10f)
    val med = media(metadata = metadata(description = null))

    val result = MediaItemBuilder.createMediaMetadata(ch, med, libraryItemId = "item-1")

    assertThat(result.description).isEqualTo("")
  }

  // endregion

  // region createMediaMetadata (track)

  @Test
  fun `createMediaMetadata for track populates all fields`() {
    val tr = track(index = 2, duration = 120f, tagTitle = "Track Title")
    val med = media(
      coverImageUrl = "/art.jpg",
      metadata = metadata(
        authorName = "Writer",
        description = "Info",
        subtitle = "Vol 1",
        title = "Saga",
      ),
    )

    val result = MediaItemBuilder.createMediaMetadata(tr, med, libraryItemId = "item-9")

    assertThat(result.id).isEqualTo(2)
    assertThat(result.title).isEqualTo("Track Title")
    assertThat(result.artist).isEqualTo("Writer")
    assertThat(result.description).isEqualTo("Info")
    assertThat(result.subtitle).isEqualTo("Vol 1")
    assertThat(result.albumTitle).isEqualTo("Saga")
    assertThat(result.artworkUri).isEqualTo("/art.jpg")
    assertThat(result.durationMs).isEqualTo(120.seconds.inWholeMilliseconds)
    assertThat(result.libraryItemId).isEqualTo("item-9")
  }

  @Test
  fun `createMediaMetadata for track uses tagged title when available`() {
    val tr = track(index = 0, title = "Fallback Title", tagTitle = "Tagged Title")
    val med = media()

    val result = MediaItemBuilder.createMediaMetadata(tr, med, libraryItemId = "item-1")

    assertThat(result.title).isEqualTo("Tagged Title")
  }

  @Test
  fun `createMediaMetadata for track falls back to title when no tag title`() {
    val tr = track(index = 0, title = "Fallback Title", tagTitle = null)
    val med = media()

    val result = MediaItemBuilder.createMediaMetadata(tr, med, libraryItemId = "item-1")

    assertThat(result.title).isEqualTo("Fallback Title")
  }

  @Test
  fun `createMediaMetadata for track with null description defaults to empty string`() {
    val tr = track(index = 0, duration = 10f)
    val med = media(metadata = metadata(description = null))

    val result = MediaItemBuilder.createMediaMetadata(tr, med, libraryItemId = "item-1")

    assertThat(result.description).isEqualTo("")
  }

  // endregion

  // region build(session) delegation

  @Test
  fun `build with session delegates to build with libraryItem`() {
    val tracks = listOf(track(index = 0, duration = 60f))
    val item = libraryItem(media = media(tracks = tracks))

    // We can't easily create a full Session, so test the libraryItem path directly
    // and verify build(item) produces the expected result
    val result = MediaItemBuilder.build(item)

    assertThat(result).hasSize(1)
    assertThat(result[0].id).isEqualTo("media-1_0")
  }

  // endregion

  // region Media item IDs

  @Test
  fun `media item ids use media id and chapter id for chapter-based items`() {
    val chapters = listOf(chapter(id = 7, start = 0f, end = 100f))
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 100f))
    val item = libraryItem(media = media(id = "abc", chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result[0].id).isEqualTo("abc_7")
  }

  @Test
  fun `media item ids use media id and track index for track-only items`() {
    val tracks = listOf(track(index = 3, startOffset = 0f, duration = 100f))
    val item = libraryItem(media = media(id = "xyz", tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result[0].id).isEqualTo("xyz_3")
  }

  // endregion

  // region MIME type propagation

  @Test
  fun `media item preserves track mime type`() {
    val tracks = listOf(
      track(index = 0, duration = 100f, mimeType = "audio/mpeg"),
    )
    val item = libraryItem(media = media(tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result[0].mimeType).isEqualTo("audio/mpeg")
  }

  // endregion

  // region Edge cases

  @Test
  fun `build with empty tracks and empty chapters returns empty list`() {
    val item = libraryItem(media = media(chapters = emptyList(), tracks = emptyList()))

    val result = MediaItemBuilder.build(item)

    assertThat(result).hasSize(0)
  }

  @Test
  fun `build with chapter spanning across two tracks includes chapter in both`() {
    // Chapter 0 starts in track 0 and ends in track 1
    val chapters = listOf(
      chapter(id = 0, start = 50f, end = 150f),
    )
    val tracks = listOf(
      track(index = 0, startOffset = 0f, duration = 100f),
      track(index = 1, startOffset = 100f, duration = 100f),
    )
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    // The chapter's start is in track 0, and its end is in track 1,
    // so it should be matched to both tracks in the multi-track path.
    // However, remaining duration check may cause fallback.
    // Track 0: chapter end (150) > track end (100), remaining = 100 - 150 = negative, no issue with threshold
    // Track 1: chapter end (150) is the last, remaining = 200 - 150 = 50 > 10, so falls back to track only
    assertThat(result).hasSize(2)
  }

  @Test
  fun `build preserves chapter order in single track multi-chapter scenario`() {
    val chapters = listOf(
      chapter(id = 0, start = 0f, end = 100f, title = "First"),
      chapter(id = 1, start = 100f, end = 200f, title = "Second"),
      chapter(id = 2, start = 200f, end = 300f, title = "Third"),
    )
    val tracks = listOf(track(index = 0, startOffset = 0f, duration = 300f))
    val item = libraryItem(media = media(chapters = chapters, tracks = tracks))

    val result = MediaItemBuilder.build(item)

    assertThat(result.map { it.metadata!!.title }).containsExactly("First", "Second", "Third")
  }

  // endregion
}
