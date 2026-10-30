// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.home.ui.libraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import kotlin.test.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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

  private val ebookOnly = libraryItem(id = "ebook")

  /** Lookups so far, by item. */
  private val lookups = mutableMapOf<LibraryItemId, Int>()

  // As the repository finds them: a book from the cache, an ebook-only book from the server
  private suspend fun item(id: LibraryItemId): LibraryItem {
    lookups[id] = (lookups[id] ?: 0) + 1
    if (id == ebookOnly.id) delay(LOOKUP_MS)
    return (items + (ebookOnly.id to ebookOnly))[id] ?: error("No item $id")
  }

  private val audiobookDownloaded =
    Download(DownloadContent.Audiobook("book"), Download.State.Downloaded, updatedAtMs = 2)

  private fun ebookAt(percent: Int) = Download(
    content = DownloadContent.Ebook(ebookOnly.id),
    state = Download.State.Downloading,
    bytes = percent * 1_000L,
    totalBytes = 100_000L,
    progress = percent / 100f,
    updatedAtMs = 1,
  )

  @Test
  fun eachKindOfDownload_getsItsRow_newestFirst() {
    val audiobook = Download(DownloadContent.Audiobook("book"), Download.State.Downloaded, updatedAtMs = 1)
    val ebook = Download(DownloadContent.Ebook("book"), Download.State.Downloading, updatedAtMs = 3)
    val episodeDownload = Download(DownloadContent.Episode("podcast", "ep_1"), Download.State.Queued, updatedAtMs = 2)

    val entries = listOf(audiobook, ebook, episodeDownload).asDownloadEntries(items)

    assertThat(entries).containsExactly(
      DownloadEntry.Ebook(book, ebook),
      DownloadEntry.Episode(podcast, episode, episodeDownload),
      DownloadEntry.Book(book, audiobook),
    )
  }

  @Test
  fun downloadsWithoutTheirItemOrEpisode_areLeftOut() {
    val entries = listOf(
      Download(DownloadContent.Audiobook("deleted"), Download.State.Downloaded),
      Download(DownloadContent.Audiobook("not_found"), Download.State.Downloaded),
      Download(DownloadContent.Episode("podcast", "gone"), Download.State.Downloaded),
      Download(DownloadContent.Episode("book", "ep_1"), Download.State.Downloaded),
    ).asDownloadEntries(items + ("not_found" to null))

    assertThat(entries).isEmpty()
  }

  @Test
  fun rowsShow_whileAnEbookDownloads() = runTest {
    val downloads = MutableStateFlow(listOf(audiobookDownloaded, ebookAt(0)))
    val shownAt = mutableMapOf<LibraryItemId, Long>()
    val collecting = launch {
      downloads.asDownloadEntries(::item).collect { entries ->
        entries.forEach { shownAt.getOrPut(it.libraryItem.id) { testScheduler.currentTime } }
      }
    }

    // An ebook download reports about every percent
    for (percent in 1..100) {
      delay(TICK_MS)
      downloads.value = listOf(audiobookDownloaded, ebookAt(percent))
    }
    collecting.cancel()

    // The book's row doesn't wait for the ebook's item, and the ebook's waits only for its own
    assertThat(shownAt).containsOnly("book" to 0L, ebookOnly.id to LOOKUP_MS)
  }

  @Test
  fun eachItem_isLookedUpOnce_andOneNotFound_isLeftOut() = runTest {
    val downloads = MutableStateFlow(listOf(ebookAt(0)))
    var entries = emptyList<DownloadEntry>()
    val collecting = launch { downloads.asDownloadEntries(::item).collect { entries = it } }

    val deleted = Download(DownloadContent.Audiobook("deleted"), Download.State.Downloaded)
    for (percent in 1..10) {
      delay(TICK_MS)
      downloads.value = listOf(ebookAt(percent), deleted)
    }
    collecting.cancel()

    assertThat(lookups).containsOnly(ebookOnly.id to 1, "deleted" to 1)
    assertThat(entries.map { it.libraryItem.id }).containsExactly(ebookOnly.id)
  }
}

private const val LOOKUP_MS = 300L
private const val TICK_MS = 100L
