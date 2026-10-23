// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.model.LibraryItemId
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.reader.api.Ebook
import app.campfire.reader.api.EbookDownload
import app.campfire.reader.api.EbookRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class EbookDownloaderTest {

  private val repository = FakeEbookRepository()
  private val downloader = EbookDownloader(repository)

  @Test
  fun `handles ebooks only`() {
    assertThat(downloader.handles(DownloadContent.Ebook("book"))).isTrue()
    assertThat(downloader.handles(DownloadContent.Audiobook("book"))).isFalse()
  }

  @Test
  fun `every kept ebook shows as a download`() = runTest {
    repository.downloads.value = mapOf(
      "book-1" to EbookDownload(EbookDownload.State.Downloaded, bytes = 100, totalBytes = 100),
      "book-2" to EbookDownload(EbookDownload.State.Downloading, bytes = 25, totalBytes = 100),
    )

    val downloads = downloader.observeAll().first()

    assertThat(downloads.map { it.content to it.state }).containsExactly(
      DownloadContent.Ebook("book-1") to Download.State.Downloaded,
      DownloadContent.Ebook("book-2") to Download.State.Downloading,
    )
    assertThat(downloads.last().progress).isEqualTo(0.25f)
  }

  @Test
  fun `no ebooks kept is empty`() = runTest {
    assertThat(downloader.observeAll().first()).isEmpty()
  }

  @Test
  fun `observing ebooks leaves out those not downloaded`() = runTest {
    repository.downloads.value = mapOf("book-1" to EbookDownload(EbookDownload.State.Failed))

    val downloads = downloader.observe(
      setOf(DownloadContent.Ebook("book-1"), DownloadContent.Ebook("book-2"), DownloadContent.Audiobook("book-1")),
    ).first()

    assertThat(downloads.mapValues { it.value.state })
      .containsOnly(DownloadContent.Ebook("book-1") to Download.State.Failed)
    assertThat(downloader.observe(setOf(DownloadContent.Audiobook("book-1"))).first()).isEmpty()
  }

  @Test
  fun `changes go to the repository`() = runTest {
    downloader.download(listOf(DownloadContent.Ebook("book-1"), DownloadContent.Ebook("book-2")))
    downloader.stop(DownloadContent.Ebook("book-1"))
    downloader.delete(DownloadContent.Ebook("book-1"))
    downloader.deleteAll("book-2")

    assertThat(repository.invocations).containsExactly(
      "download book-1",
      "download book-2",
      "deleteDownload book-1",
      "deleteFiles book-2",
    )
  }

  @Test
  fun `an ebook of unknown size has no progress`() {
    val download = EbookDownload(EbookDownload.State.Downloading, bytes = 25).asDownload("book")

    assertThat(download?.progress).isNull()
    assertThat(EbookDownload.None.asDownload("book")).isNull()
  }
}

private class FakeEbookRepository : EbookRepository {

  val downloads = MutableStateFlow<Map<LibraryItemId, EbookDownload>>(emptyMap())
  val invocations = mutableListOf<String>()

  override suspend fun open(itemId: LibraryItemId): Result<Ebook> =
    Result.failure(NotImplementedError("Not used by these tests"))

  override suspend fun saveProgress(itemId: LibraryItemId, location: String, progress: Float): Result<Unit> =
    Result.success(Unit)

  override fun observeDownload(itemId: LibraryItemId): Flow<EbookDownload> =
    downloads.map { it[itemId] ?: EbookDownload.None }

  override fun observeDownloads(): Flow<Set<LibraryItemId>> = downloads.map { it.keys }

  override fun download(itemId: LibraryItemId) {
    invocations += "download $itemId"
  }

  override fun deleteDownload(itemId: LibraryItemId) {
    invocations += "deleteDownload $itemId"
  }

  override suspend fun deleteFiles(itemId: LibraryItemId) {
    invocations += "deleteFiles $itemId"
  }
}
