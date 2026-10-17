// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail

import app.campfire.core.model.LibraryItemId
import app.campfire.reader.api.Ebook
import app.campfire.reader.api.EbookDownload
import app.campfire.reader.api.EbookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeEbookRepository : EbookRepository {

  val download = MutableStateFlow(EbookDownload.None)
  val downloaded = mutableListOf<LibraryItemId>()
  val deleted = mutableListOf<LibraryItemId>()

  override suspend fun open(itemId: LibraryItemId): Result<Ebook> =
    Result.failure(NotImplementedError("Not used by these tests"))

  override suspend fun saveProgress(itemId: LibraryItemId, location: String, progress: Float): Result<Unit> =
    Result.success(Unit)

  override fun observeDownload(itemId: LibraryItemId): Flow<EbookDownload> = download

  override fun observeDownloads(): Flow<Set<LibraryItemId>> = MutableStateFlow(emptySet())

  override fun download(itemId: LibraryItemId) {
    downloaded += itemId
  }

  override fun deleteDownload(itemId: LibraryItemId) {
    deleted += itemId
  }

  override suspend fun deleteFiles(itemId: LibraryItemId) = Unit
}
