// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.test

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.libraries.api.LibraryItemRepository
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
}
