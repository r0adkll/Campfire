// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DownloadSectionsTest {

  private val bookItem = libraryItem(id = "book")
  private val showA = libraryItem(id = "show_a")
  private val showB = libraryItem(id = "show_b")

  private val book = DownloadEntry.Book(bookItem, downloaded(DownloadContent.Audiobook("book")))
  private val ebook = DownloadEntry.Ebook(bookItem, downloaded(DownloadContent.Ebook("book"), totalBytes = 2_000L))
  private val a1 = episode(showA.id, "a1", sizeInBytes = 100L)
  private val a2 = episode(showA.id, "a2", sizeInBytes = 50L)
  private val b1 = episode(showB.id, "b1", sizeInBytes = 30L)
  private val downloading = DownloadEntry.Book(
    libraryItem(id = "going"),
    Download(DownloadContent.Audiobook("going"), Download.State.Downloading, bytes = 400L, totalBytes = 1_000L),
  )
  private val failedEpisode = DownloadEntry.Episode(
    showA,
    podcastEpisode(showA.id, "a3", sizeInBytes = 70L),
    Download(DownloadContent.Episode(showA.id, "a3"), Download.State.Failed),
  )
  private val unavailable = DownloadEntry.Unavailable(downloaded(DownloadContent.Audiobook("gone")), sizeInBytes = 9L)

  private val all = listOf(b1, book, downloading, a1, ebook, failedEpisode, unavailable, a2)

  @Test
  fun `finished downloads sit by kind, and the rest under in progress or unavailable`() {
    val sections = all.asSections()

    assertThat(sections.inProgress).containsExactly(downloading, failedEpisode)
    assertThat(sections.audiobooks).containsExactly(book)
    assertThat(sections.ebooks).containsExactly(ebook)
    assertThat(sections.unavailable).containsExactly(unavailable)
  }

  @Test
  fun `episodes sit under their show, in the order they were given`() {
    val sections = all.asSections()

    assertThat(sections.shows).containsExactly(
      ShowDownloads(showB, listOf(b1)),
      ShowDownloads(showA, listOf(a1, a2)),
    )
    assertThat(sections.shows.last().sizeInBytes).isEqualTo(150L)
  }

  @Test
  fun `one kind narrows every section to it`() {
    val sections = all.asSections(DownloadKind.Podcast)

    assertThat(sections.inProgress).containsExactly(failedEpisode)
    assertThat(sections.audiobooks).isEmpty()
    assertThat(sections.ebooks).isEmpty()
    assertThat(sections.unavailable).isEmpty()
    assertThat(sections.shows.flatMap { it.episodes }).containsExactly(b1, a1, a2)
  }

  @Test
  fun `a download without its item still counts toward its kind`() {
    assertThat(all.asSections(DownloadKind.Audiobook).unavailable).containsExactly(unavailable)
  }

  @Test
  fun `the summary counts everything, by what's on the device`() {
    val summary = all.summary()

    assertThat(summary.count).isEqualTo(8)
    assertThat(summary.kinds).containsExactly(
      // The finished book, what's fetched of the one going, and what's kept for the missing one
      KindSummary(DownloadKind.Audiobook, count = 3, sizeInBytes = bookItem.media.sizeInBytes + 400L + 9L),
      // A failed episode takes up nothing yet
      KindSummary(DownloadKind.Podcast, count = 4, sizeInBytes = 180L),
      KindSummary(DownloadKind.Ebook, count = 1, sizeInBytes = 2_000L),
    )
    assertThat(summary.sizeInBytes).isEqualTo(summary.kinds.sumOf { it.sizeInBytes })
  }

  @Test
  fun `the summary leaves out kinds with nothing downloaded`() {
    assertThat(listOf(book).summary().kinds.map { it.kind }).containsExactly(DownloadKind.Audiobook)
    assertThat(emptyList<DownloadEntry>().summary()).isEqualTo(DownloadsSummary(0, 0L, emptyList()))
  }

  private fun downloaded(content: DownloadContent, totalBytes: Long = -1L) =
    Download(content, Download.State.Downloaded, totalBytes = totalBytes)

  private fun episode(showId: String, id: String, sizeInBytes: Long) = DownloadEntry.Episode(
    libraryItem = if (showId == showA.id) showA else showB,
    episode = podcastEpisode(showId, id, sizeInBytes),
    download = downloaded(DownloadContent.Episode(showId, id)),
  )

  private fun podcastEpisode(showId: String, id: String, sizeInBytes: Long) = PodcastEpisode(
    id = id,
    libraryItemId = showId,
    podcastId = "media_$showId",
    title = "Episode $id",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = sizeInBytes,
  )
}
