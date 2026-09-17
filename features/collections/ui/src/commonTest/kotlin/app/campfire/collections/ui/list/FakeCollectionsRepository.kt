// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections.ui.list

import app.campfire.collections.api.CollectionsRepository
import app.campfire.core.model.Collection
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeCollectionsRepository(
  private val onRefreshCollections: suspend () -> Unit = {},
) : CollectionsRepository {

  var refreshCount = 0
    private set

  override fun observeAllCollections(): Flow<List<Collection>> = emptyFlow()

  override suspend fun refreshCollections() {
    refreshCount++
    onRefreshCollections()
  }

  override fun observeCollection(collectionId: CollectionId): Flow<Collection> = TODO()

  override fun observeCollectionItems(collectionId: CollectionId): Flow<List<LibraryItem>> = TODO()

  override suspend fun createCollection(
    name: String,
    bookIds: List<String>,
    description: String?,
  ): Result<CollectionId> = TODO()

  override suspend fun updateCollection(
    collectionId: CollectionId,
    name: String?,
    description: String?,
  ): Result<Unit> = TODO()

  override suspend fun deleteCollection(collectionId: CollectionId): Result<Unit> = TODO()

  override suspend fun addToCollection(bookId: LibraryItemId, collectionId: CollectionId): Result<Unit> = TODO()

  override suspend fun removeFromCollection(bookIds: List<LibraryItemId>, collectionId: CollectionId): Result<Unit> =
    TODO()
}
