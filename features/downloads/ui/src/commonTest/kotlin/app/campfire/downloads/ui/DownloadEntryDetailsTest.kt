// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class DownloadEntryDetailsTest {

  @Test
  fun `an audiobook is named by its one file format`() {
    assertThat(DownloadEntry.Book(audiobook(".m4b", ".m4b"), download()).formatLabel).isEqualTo("M4B")
  }

  @Test
  fun `an audiobook mixing formats isn't named by any`() {
    assertThat(DownloadEntry.Book(audiobook(".mp3", ".m4a"), download()).formatLabel).isNull()
  }

  @Test
  fun `an ebook is named by its format`() {
    val entry = DownloadEntry.Ebook(libraryItem(numTracks = 0, ebookFormat = "epub"), download())

    assertThat(entry.formatLabel).isEqualTo("EPUB")
  }

  @Test
  fun `an episode and a download without its item have no format`() {
    assertThat(DownloadEntry.Episode(libraryItem(), episode(sizeInBytes = 1L), download()).formatLabel).isNull()
    assertThat(DownloadEntry.Unavailable(download(), sizeInBytes = 1L).formatLabel).isNull()
  }

  @Test
  fun `an ebook takes up its own file, not the whole item`() {
    val item = libraryItem(numTracks = 0, ebookFormat = "epub")

    assertThat(DownloadEntry.Ebook(item, download(contentLength = 2048L)).sizeInBytes).isEqualTo(2048L)
    // Until its size is known
    assertThat(DownloadEntry.Ebook(item, download()).sizeInBytes).isEqualTo(item.media.sizeInBytes)
  }

  @Test
  fun `a book takes up its item's size and an episode its own`() {
    val item = libraryItem()

    assertThat(DownloadEntry.Book(item, download()).sizeInBytes).isEqualTo(item.media.sizeInBytes)
    assertThat(DownloadEntry.Episode(item, episode(sizeInBytes = 4096L), download()).sizeInBytes).isEqualTo(4096L)
    assertThat(DownloadEntry.Unavailable(download(), sizeInBytes = 512L).sizeInBytes).isEqualTo(512L)
  }

  @Test
  fun `progress reads as a whole percent, when it can be told`() {
    val item = libraryItem()

    assertThat(DownloadEntry.Book(item, download().copy(progress = 0.456f)).percent).isEqualTo(46)
    assertThat(DownloadEntry.Book(item, download().copy(progress = 1.2f)).percent).isEqualTo(100)
    assertThat(DownloadEntry.Book(item, download()).percent).isNull()
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
