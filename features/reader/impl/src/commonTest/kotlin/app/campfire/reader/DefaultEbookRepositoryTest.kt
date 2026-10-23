// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.reader.EbookFilesFixture.Companion.BOOK
import app.campfire.reader.EbookFilesFixture.Companion.ITEM
import app.campfire.user.test.FakeMediaProgressRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.SystemFileSystem

class DefaultEbookRepositoryTest {

  private val fixture = EbookFilesFixture()

  @AfterTest
  fun cleanUp() = fixture.cleanUp()

  @Test
  fun open_fetchesTheBookOnce_withTheAccountsCredentials() = runTest {
    val repository = fixture.repository()

    val first = repository.open(ITEM).getOrThrow()
    val second = repository.open(ITEM).getOrThrow()

    assertThat(first.filePath).isEqualTo(fixture.cached().toString())
    assertThat(second.filePath).isEqualTo(first.filePath)
    assertThat(fixture.readFile(first.filePath)).isEqualTo(BOOK.decodeToString())
    assertThat(fixture.requests).containsExactly(
      mapOf("url" to "items/$ITEM/ebook", "auth" to "Bearer access", "extra" to "value"),
    )
  }

  @Test
  fun open_refreshesAnExpiredTokenOnce() = runTest {
    fixture.responses = listOf(HttpStatusCode.Unauthorized, HttpStatusCode.OK)

    fixture.repository().open(ITEM).getOrThrow()

    assertThat(fixture.requests.map { it["auth"] }).containsExactly("Bearer access", "Bearer refreshed")
  }

  @Test
  fun open_failsOnAServerError_withoutLeavingAFileBehind() = runTest {
    fixture.responses = listOf(HttpStatusCode.InternalServerError)

    val result = fixture.repository().open(ITEM)

    assertThat(result.isFailure).isTrue()
    assertThat(SystemFileSystem.exists(fixture.cached())).isFalse()
  }

  @Test
  fun open_prefersADownloadedBook() = runTest {
    val files = fixture.files()
    val downloader = fixture.downloader(files)
    downloader.download(listOf(DownloadContent.Ebook(ITEM)))
    downloader.observe(setOf(DownloadContent.Ebook(ITEM)))
      .first { it.values.singleOrNull()?.state == Download.State.Downloaded }

    val ebook = fixture.repository(files).open(ITEM).getOrThrow()

    assertThat(ebook.filePath).isEqualTo(fixture.downloaded().toString())
    assertThat(fixture.requests.size).isEqualTo(1)
  }

  @Test
  fun saveProgress_savesThroughTheProgressRepository() = runTest {
    fixture.repository().saveProgress(ITEM, "epubcfi(/6/4!/4/2/1:0)", 0.4f)

    assertThat(fixture.mediaProgress.invocations).containsExactly(
      FakeMediaProgressRepository.Invocation.UpdateEbookProgress(ITEM, "epubcfi(/6/4!/4/2/1:0)", 0.4f),
    )
  }
}
