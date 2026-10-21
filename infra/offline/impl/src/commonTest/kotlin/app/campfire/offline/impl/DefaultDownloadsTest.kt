// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline.impl

import app.campfire.core.model.LibraryItemId
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.Downloader
import app.campfire.offline.ItemDownloads
import app.campfire.offline.download
import app.campfire.offline.observe
import app.campfire.offline.observeByItem
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull

class DefaultDownloadsTest {

  private val audio = FakeDownloader { it is DownloadContent.Audiobook || it is DownloadContent.Episode }
  private val ebooks = FakeDownloader { it is DownloadContent.Ebook }
  private val downloads = DefaultDownloads(setOf(audio, ebooks))

  private val book = DownloadContent.Audiobook("book")
  private val episode = DownloadContent.Episode("podcast", "ep-1")
  private val ebook = DownloadContent.Ebook("book")

  @Test
  fun `every downloader's downloads show together`() = runTest {
    audio.downloads.value = listOf(Download(book, Download.State.Downloaded))
    ebooks.downloads.value = listOf(Download(ebook, Download.State.Downloading))

    assertThat(downloads.observeAll().first().map { it.content }).containsExactlyInAnyOrder(book, ebook)
  }

  @Test
  fun `nothing shows until every downloader has read its downloads`() = runTest {
    val loading = FakeDownloader(all = MutableSharedFlow()) { it is DownloadContent.Ebook }
    audio.downloads.value = listOf(Download(book, Download.State.Downloaded))

    val all = DefaultDownloads(setOf(audio, loading)).observeAll()

    assertThat(withTimeoutOrNull(100.milliseconds) { all.first() }).isNull()
  }

  @Test
  fun `no downloaders keep nothing`() = runTest {
    val none = DefaultDownloads(emptySet())

    assertThat(none.observeAll().first()).isEmpty()
    assertThat(none.observe(setOf(book)).first()).isEmpty()
    none.download(book)
  }

  @Test
  fun `downloads group by item`() = runTest {
    audio.downloads.value = listOf(Download(book, Download.State.Downloaded))
    ebooks.downloads.value = listOf(Download(ebook, Download.State.Queued))

    assertThat(downloads.observeByItem().first()).containsOnly(
      "book" to ItemDownloads(
        itemId = "book",
        audiobook = Download(book, Download.State.Downloaded),
        ebook = Download(ebook, Download.State.Queued),
      ),
    )
  }

  @Test
  fun `observing content asks the downloader that keeps it`() = runTest {
    audio.downloads.value = listOf(
      Download(book, Download.State.Downloading, progress = 0.5f),
      Download(episode, Download.State.Downloaded),
    )
    ebooks.downloads.value = listOf(Download(ebook, Download.State.Failed))

    val observed = downloads.observe(setOf(book, ebook, DownloadContent.Ebook("missing"))).first()

    assertThat(observed.mapValues { it.value.state }).containsOnly(
      book to Download.State.Downloading,
      ebook to Download.State.Failed,
    )
    assertThat(audio.observed).containsExactly(setOf(book))
    assertThat(ebooks.observed).containsExactly(setOf(ebook, DownloadContent.Ebook("missing")))
  }

  @Test
  fun `one content's download is null until it's downloaded`() = runTest {
    assertThat(downloads.observe(book).first()).isNull()

    audio.downloads.value = listOf(Download(book, Download.State.Queued))

    assertThat(downloads.observe(book).first()?.state).isEqualTo(Download.State.Queued)
  }

  @Test
  fun `changes go to the downloader that keeps the content`() = runTest {
    downloads.download(listOf(book, ebook, episode))
    downloads.stop(episode)
    downloads.delete(ebook)

    assertThat(audio.invocations).containsExactly("download [$book, $episode]", "stop $episode")
    assertThat(ebooks.invocations).containsExactly("download [$ebook]", "delete $ebook")
  }

  @Test
  fun `deleting an item goes to every downloader`() = runTest {
    downloads.deleteAll("book")

    assertThat(audio.invocations).containsExactly("deleteAll book")
    assertThat(ebooks.invocations).containsExactly("deleteAll book")
  }
}

private class FakeDownloader(
  val downloads: MutableStateFlow<List<Download>> = MutableStateFlow(emptyList()),
  private val all: Flow<List<Download>> = downloads,
  private val handles: (DownloadContent) -> Boolean,
) : Downloader {

  val invocations = mutableListOf<String>()
  val observed = mutableListOf<Set<DownloadContent>>()

  override fun handles(content: DownloadContent): Boolean = handles.invoke(content)

  override fun observeAll(): Flow<List<Download>> = all

  override fun observe(contents: Set<DownloadContent>): Flow<Map<DownloadContent, Download>> {
    observed += contents
    return downloads.map { all -> all.filter { it.content in contents }.associateBy { it.content } }
  }

  override fun download(contents: List<DownloadContent>) {
    invocations += "download $contents"
  }

  override fun stop(content: DownloadContent) {
    invocations += "stop $content"
  }

  override fun delete(content: DownloadContent) {
    invocations += "delete $content"
  }

  override suspend fun deleteAll(itemId: LibraryItemId) {
    invocations += "deleteAll $itemId"
  }
}
