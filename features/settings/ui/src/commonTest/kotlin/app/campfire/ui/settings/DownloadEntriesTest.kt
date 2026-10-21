// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.home.ui.libraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class DownloadEntriesTest {

  private val book = libraryItem(id = "book")
  private val episode = PodcastEpisode(
    id = "ep_1",
    libraryItemId = "podcast",
    podcastId = "podcast_media",
    title = "Episode One",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )
  private val podcast = libraryItem(id = "podcast").copy(
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
  )
  private val items = listOf(book, podcast).associateBy { it.id }

  private suspend fun item(id: LibraryItemId): LibraryItem = items[id] ?: error("No item $id")

  @Test
  fun eachKindOfDownload_getsItsRow_newestFirst() = runTest {
    val audiobook = Download(DownloadContent.Audiobook("book"), Download.State.Downloaded, updatedAtMs = 1)
    val ebook = Download(DownloadContent.Ebook("book"), Download.State.Downloading, updatedAtMs = 3)
    val episodeDownload = Download(DownloadContent.Episode("podcast", "ep_1"), Download.State.Queued, updatedAtMs = 2)

    val entries = listOf(audiobook, ebook, episodeDownload).asDownloadEntries(::item)

    assertThat(entries).containsExactly(
      DownloadEntry.Ebook(book, ebook),
      DownloadEntry.Episode(podcast, episode, episodeDownload),
      DownloadEntry.Book(book, audiobook),
    )
  }

  @Test
  fun downloadsWithoutTheirItemOrEpisode_areLeftOut() = runTest {
    val entries = listOf(
      Download(DownloadContent.Audiobook("deleted"), Download.State.Downloaded),
      Download(DownloadContent.Episode("podcast", "gone"), Download.State.Downloaded),
      Download(DownloadContent.Episode("book", "ep_1"), Download.State.Downloaded),
    ).asDownloadEntries(::item)

    assertThat(entries).isEmpty()
  }
}
