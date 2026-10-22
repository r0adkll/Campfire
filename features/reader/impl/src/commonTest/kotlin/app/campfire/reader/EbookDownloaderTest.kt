// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.reader.EbookFilesFixture.Companion.BOOK
import app.campfire.reader.EbookFilesFixture.Companion.ITEM
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem

class EbookDownloaderTest {

  private val fixture = EbookFilesFixture()
  private val ebook = DownloadContent.Ebook(ITEM)

  @AfterTest
  fun cleanUp() = fixture.cleanUp()

  private suspend fun EbookDownloader.awaitState(state: Download.State): Download =
    observe(setOf(ebook)).first { it[ebook]?.state == state }.getValue(ebook)

  @Test
  fun handles_ebooksOnly() {
    val downloader = fixture.downloader()

    assertThat(downloader.handles(ebook)).isTrue()
    assertThat(downloader.handles(DownloadContent.Audiobook(ITEM))).isFalse()
  }

  @Test
  fun download_movesABookAlreadyOpened() = runTest {
    val files = fixture.files()
    fixture.repository(files).open(ITEM).getOrThrow()
    val downloader = fixture.downloader(files)

    downloader.download(listOf(ebook))
    val download = downloader.awaitState(Download.State.Downloaded)

    assertThat(download.totalBytes).isEqualTo(BOOK.size.toLong())
    assertThat(download.progress).isEqualTo(1f)
    assertThat(fixture.requests.size).isEqualTo(1)
    assertThat(SystemFileSystem.exists(fixture.cached())).isFalse()
  }

  @Test
  fun download_fetchesABookNotOpened_andSurvivesARestart() = runTest {
    fixture.downloader().apply {
      download(listOf(ebook))
      awaitState(Download.State.Downloaded)
    }

    // A new session reads what's downloaded from the disk
    val restarted = fixture.downloader().observe(setOf(ebook)).first()
    assertThat(restarted[ebook]?.state).isEqualTo(Download.State.Downloaded)
    assertThat(fixture.readFile(fixture.downloaded().toString())).isEqualTo(BOOK.decodeToString())
  }

  @Test
  fun observeAll_followsDownloads_andReadsThemFromTheDiskAfterARestart() = runTest {
    fixture.downloader().apply {
      assertThat(observeAll().first()).isEmpty()
      download(listOf(ebook))
      // On its way counts
      observeAll().first { all -> all.any { it.content == ebook } }
      awaitState(Download.State.Downloaded)
    }
    // A download cut short isn't one
    SystemFileSystem.sink(fixture.downloaded("other.epub.part")).buffered().use { it.write(BOOK) }

    val restarted = fixture.downloader()
    assertThat(restarted.observeAll().first().map { it.content }).containsExactly(ebook)

    restarted.delete(ebook)
    assertThat(restarted.observeAll().first()).isEmpty()
  }

  @Test
  fun aDownload_keepsTheTimeItFinished_afterARestart() = runTest {
    // Opened first, so the download moves a file written today rather than at the clock's time
    val files = fixture.files()
    fixture.repository(files).open(ITEM).getOrThrow()
    fixture.time.nowMillis = FINISHED_AT
    fixture.downloader(files).apply {
      download(listOf(ebook))
      assertThat(awaitState(Download.State.Downloaded).updatedAtMs).isEqualTo(FINISHED_AT)
    }

    fixture.time.nowMillis = FINISHED_AT + 60_000
    assertThat(fixture.downloader().observeAll().first().single().updatedAtMs).isEqualTo(FINISHED_AT)
  }

  @Test
  fun aFailedDownload_hasTheTimeItFailed() = runTest {
    fixture.responses = listOf(HttpStatusCode.Forbidden)
    fixture.time.nowMillis = FINISHED_AT
    val downloader = fixture.downloader()

    downloader.download(listOf(ebook))

    assertThat(downloader.awaitState(Download.State.Failed).updatedAtMs).isEqualTo(FINISHED_AT)
  }

  @Test
  fun download_reportsAFailure_onItsItemOnly() = runTest {
    fixture.responses = listOf(HttpStatusCode.Forbidden)
    val downloader = fixture.downloader()

    downloader.download(listOf(ebook))

    assertThat(downloader.awaitState(Download.State.Failed).isAvailable).isFalse()
    assertThat(downloader.observeAll().first()).isEmpty()
  }

  @Test
  fun stop_isIgnored() = runTest {
    val downloader = fixture.downloader()
    downloader.download(listOf(ebook))
    downloader.stop(ebook)

    assertThat(downloader.awaitState(Download.State.Downloaded).isAvailable).isTrue()
  }

  @Test
  fun deleting_removesTheDownload_andDeletingTheItemRemovesEveryCopy() = runTest {
    val files = fixture.files()
    val downloader = fixture.downloader(files)
    downloader.download(listOf(ebook))
    downloader.awaitState(Download.State.Downloaded)

    downloader.delete(ebook)
    assertThat(downloader.observe(setOf(ebook)).first()).isEmpty()

    fixture.repository(files).open(ITEM).getOrThrow()
    downloader.deleteAll(ITEM)

    assertThat(SystemFileSystem.exists(fixture.cached())).isFalse()
    assertThat(SystemFileSystem.exists(fixture.downloaded())).isFalse()
  }

  @Test
  fun observing_nothingButEbooks_isEmpty() = runTest {
    assertThat(fixture.downloader().observe(setOf(DownloadContent.Audiobook(ITEM))).first()).isEmpty()
  }
}

// Whole seconds, which every file system keeps
private const val FINISHED_AT = 1_700_000_000_000L
