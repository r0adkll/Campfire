// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.test

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.RescanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeLibraryItemRepository : LibraryItemRepository {

  val libraryItemFlow = MutableSharedFlow<LibraryItem>(replay = 1)
  override fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem> {
    return libraryItemFlow
  }

  lateinit var libraryItem: LibraryItem
  override suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem {
    return libraryItem
  }

  val deleteInvocations = mutableListOf<Pair<LibraryItemId, Boolean>>()
  var deleteResult: Result<Unit> = Result.success(Unit)
  override suspend fun deleteLibraryItem(itemId: LibraryItemId, hardDelete: Boolean): Result<Unit> {
    deleteInvocations += itemId to hardDelete
    return deleteResult
  }

  val rescanInvocations = mutableListOf<LibraryItemId>()
  var rescanResult: Result<RescanResult> = Result.success(RescanResult.UpToDate)
  override suspend fun rescanLibraryItem(itemId: LibraryItemId): Result<RescanResult> {
    rescanInvocations += itemId
    return rescanResult
  }

  val detailUpdates = mutableListOf<Pair<LibraryItemId, BookDetailsUpdate>>()
  var updateBookDetailsResult: Result<Boolean> = Result.success(true)
  override suspend fun updateBookDetails(itemId: LibraryItemId, update: BookDetailsUpdate): Result<Boolean> {
    detailUpdates += itemId to update
    return updateBookDetailsResult
  }
}
