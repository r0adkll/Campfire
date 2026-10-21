// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.ui.settings.DownloadEntry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class DownloadEntryTextTest {

  private val bookSize = libraryItem().media.sizeInBytes.asReadableBytes()

  @Test
  fun `an audiobook shows its format only next to other kinds of downloads`() {
    val entry = DownloadEntry.Book(audiobook(".m4b"), download())

    assertThat(entry.supportingText(showFormat = true)).isEqualTo("M4B · $bookSize")
    assertThat(entry.supportingText(showFormat = false)).isEqualTo(bookSize)
  }

  @Test
  fun `an audiobook mixing formats shows just its size`() {
    val entry = DownloadEntry.Book(audiobook(".mp3", ".m4a"), download())

    assertThat(entry.supportingText(showFormat = true)).isEqualTo(bookSize)
  }

  @Test
  fun `an ebook always shows its format and downloaded size`() {
    val entry = DownloadEntry.Ebook(
      libraryItem(numTracks = 0, ebookFormat = "epub"),
      download(contentLength = 2048L),
    )

    assertThat(entry.supportingText(showFormat = false)).isEqualTo("EPUB · ${2048L.asReadableBytes()}")
  }

  @Test
  fun `an ebook whose size isn't known yet falls back to the item's`() {
    val entry = DownloadEntry.Ebook(libraryItem(numTracks = 0, ebookFormat = "cbz"), download())

    assertThat(entry.supportingText(showFormat = true)).isEqualTo("CBZ · $bookSize")
  }

  @Test
  fun `an episode shows its podcast and size`() {
    val item = libraryItem()
    val entry = DownloadEntry.Episode(item, episode(sizeInBytes = 4096L), download())

    assertThat(entry.supportingText(showFormat = true))
      .isEqualTo("${item.media.metadata.title} · ${4096L.asReadableBytes()}")
  }

  @Test
  fun `only a list of different kinds of downloads is mixed`() {
    val book = DownloadEntry.Book(audiobook(".m4b"), download())
    val ebook = DownloadEntry.Ebook(libraryItem(numTracks = 0, ebookFormat = "epub"), download())

    assertThat(listOf(book, book.copy()).hasMixedMediaTypes).isFalse()
    assertThat(listOf(ebook).hasMixedMediaTypes).isFalse()
    assertThat(listOf(book, ebook).hasMixedMediaTypes).isTrue()
  }

  private fun download(contentLength: Long = -1L) = Download(
    content = DownloadContent.Audiobook("item"),
    state = Download.State.Downloaded,
    totalBytes = contentLength,
  )

  private fun audiobook(vararg extensions: String): LibraryItem {
    val item = libraryItem()
    val media = item.media as Media.Book
    return item.copy(media = media.copy(tracks = extensions.mapIndexed { index, ext -> track(index, ext) }))
  }

  private fun track(index: Int, ext: String) = AudioTrack(
    index = index,
    startOffset = 0f,
    duration = 60f,
    title = "Track $index",
    contentUrl = "",
    mimeType = "audio/mp4",
    codec = "aac",
    metadata = FileMetadata(
      filename = "track$index$ext",
      ext = ext,
      path = "",
      relPath = "",
      size = 0L,
      mtimeMs = 0L,
      ctimeMs = 0L,
      birthtimeMs = 0L,
    ),
    metaTags = null,
  )

  private fun episode(sizeInBytes: Long) = PodcastEpisode(
    id = "episode",
    libraryItemId = "item",
    podcastId = "podcast",
    title = "Episode",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = sizeInBytes,
  )
}
