// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.model.LibraryItemId
import app.campfire.reader.api.Ebook
import app.campfire.reader.api.EbookDownload
import app.campfire.reader.api.EbookRepository
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class EbookDownloadsTest {

  private val repository = FakeEbookRepository()

  @Test
  fun `no ebook downloads is an empty list`() = runTest {
    assertThat(repository.observeOfflineDownloads().first()).isEmpty()
  }

  @Test
  fun `each downloaded ebook shows as a download`() = runTest {
    repository.downloads.value = mapOf(
      "book-1" to EbookDownload(EbookDownload.State.Downloaded, bytes = 100, totalBytes = 100),
      "book-2" to EbookDownload(EbookDownload.State.Downloading, bytes = 25, totalBytes = 100),
    )

    val downloads = repository.observeOfflineDownloads().first()

    assertThat(downloads.map { it.libraryItemId to it.state }).containsExactlyInAnyOrder(
      "book-1" to OfflineDownload.State.Completed,
      "book-2" to OfflineDownload.State.Downloading,
    )
    assertThat(downloads.first { it.libraryItemId == "book-2" }.progress.percent).isEqualTo(0.25f)
  }

  @Test
  fun `a deleted ebook download drops out`() = runTest {
    repository.downloads.value = mapOf("book-1" to EbookDownload(EbookDownload.State.Downloaded))

    repository.observeOfflineDownloads().first { it.size == 1 }
    repository.downloads.value = emptyMap()

    assertThat(repository.observeOfflineDownloads().first()).isEmpty()
  }
}

private class FakeEbookRepository : EbookRepository {

  val downloads = MutableStateFlow<Map<LibraryItemId, EbookDownload>>(emptyMap())

  override suspend fun open(itemId: LibraryItemId): Result<Ebook> =
    Result.failure(NotImplementedError("Not used by these tests"))

  override suspend fun saveProgress(itemId: LibraryItemId, location: String, progress: Float): Result<Unit> =
    Result.success(Unit)

  override fun observeDownload(itemId: LibraryItemId): Flow<EbookDownload> =
    downloads.map { it[itemId] ?: EbookDownload.None }

  override fun observeDownloads(): Flow<Set<LibraryItemId>> = downloads.map { it.keys }

  override fun download(itemId: LibraryItemId) = Unit

  override fun deleteDownload(itemId: LibraryItemId) = Unit

  override suspend fun deleteFiles(itemId: LibraryItemId) = Unit
}
