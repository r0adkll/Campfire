// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui

import app.campfire.common.test.session
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SessionDisplayStateTest {

  @Test
  fun `book with chapters shows chapter-relative time and chapter metadata`() {
    // 10 hours split evenly across 10 chapters; 90 minutes in = 30 minutes into chapter 2
    val session = session(
      libraryItem = libraryItem(duration = 10.hours, numOfChapters = 10),
      currentTime = 90.minutes,
    )

    assertThat(session.placeholderDisplayState()).all {
      prop(SessionDisplayState::time).isEqualTo(30.minutes)
      prop(SessionDisplayState::bookTime).isEqualTo(90.minutes)
      prop(SessionDisplayState::duration).isEqualTo(1.hours)
      transform { it.metadata.title }.isEqualTo("Chapter 2")
    }
  }

  @Test
  fun `chapterless book with tracks shows track-relative time and track metadata`() {
    val session = session(
      libraryItem = libraryItem(duration = 2.hours, numOfChapters = 0)
        .withTracks(trackCount = 2, duration = 2.hours),
      currentTime = 90.minutes,
    )

    assertThat(session.placeholderDisplayState()).all {
      prop(SessionDisplayState::time).isEqualTo(30.minutes)
      prop(SessionDisplayState::bookTime).isEqualTo(90.minutes)
      prop(SessionDisplayState::duration).isEqualTo(1.hours)
      transform { it.metadata.title }.isEqualTo("Track 2")
    }
  }

  @Test
  fun `book without chapters or tracks shows absolute time against the full duration`() {
    val session = session(
      libraryItem = libraryItem(duration = 3.hours, numOfChapters = 0),
      currentTime = 45.minutes,
    )

    assertThat(session.placeholderDisplayState()).all {
      prop(SessionDisplayState::time).isEqualTo(45.minutes)
      prop(SessionDisplayState::bookTime).isEqualTo(45.minutes)
      prop(SessionDisplayState::duration).isEqualTo(3.hours)
      transform { it.metadata.title }.isEqualTo("Dungeon Crawler Carl")
    }
  }

  @Test
  fun `podcast episode shows episode-relative time and episode metadata`() {
    val base = libraryItem(numOfChapters = 0)
    val episode = PodcastEpisode(
      id = "ep1",
      libraryItemId = base.id,
      podcastId = "podcast_media",
      title = "Episode One",
      addedAtMillis = 0L,
      updatedAtMillis = 0L,
      durationInMillis = 1.hours.inWholeMilliseconds,
      sizeInBytes = 0L,
    )
    val session = session(
      libraryItem = base.copy(
        media = Media.Podcast(
          id = "podcast_media",
          metadata = Media.Metadata.Podcast(
            title = "The Podcast",
            author = "Author",
            description = null,
            releaseDate = null,
            genres = emptyList(),
            feedUrl = null,
            imageUrl = null,
            itunesPageUrl = null,
            itunesId = null,
            itunesArtistId = null,
            isExplicit = false,
            language = null,
            podcastType = null,
          ),
          coverImageUrl = "",
          coverPath = null,
          tags = emptyList(),
          sizeInBytes = 0L,
          episodes = listOf(episode),
        ),
      ),
      currentTime = 20.minutes,
    ).copy(episodeId = "ep1")

    assertThat(session.placeholderDisplayState()).all {
      prop(SessionDisplayState::time).isEqualTo(20.minutes)
      prop(SessionDisplayState::duration).isEqualTo(1.hours)
      transform { it.metadata.title }.isEqualTo("Episode One")
    }
  }

  @Test
  fun `invalid current time clamps to zero`() {
    val session = session(
      libraryItem = libraryItem(duration = 10.hours, numOfChapters = 10),
      currentTime = Duration.INFINITE,
    )

    assertThat(session.placeholderDisplayState()).all {
      prop(SessionDisplayState::time).isEqualTo(Duration.ZERO)
      prop(SessionDisplayState::bookTime).isEqualTo(Duration.ZERO)
      // Zero time falls in the first chapter
      prop(SessionDisplayState::duration).isEqualTo(1.hours)
    }
  }

  private fun LibraryItem.withTracks(trackCount: Int, duration: Duration): LibraryItem {
    val book = media as Media.Book
    val trackDuration = duration / trackCount
    return copy(
      media = book.copy(
        numTracks = trackCount,
        tracks = (0 until trackCount).map { index ->
          AudioTrack(
            index = index + 1,
            startOffset = (trackDuration * index).inWholeSeconds.toFloat(),
            duration = trackDuration.inWholeSeconds.toFloat(),
            title = "Track ${index + 1}",
            contentUrl = "/api/items/test/file/$index",
            mimeType = "audio/mp4",
            codec = "aac",
            metadata = FileMetadata(
              filename = "track$index.m4b",
              ext = "m4b",
              path = "/audiobooks/test/track$index.m4b",
              relPath = "track$index.m4b",
              size = 0L,
              mtimeMs = 0L,
              ctimeMs = 0L,
              birthtimeMs = 0L,
            ),
            metaTags = null,
          )
        },
      ),
    )
  }
}
