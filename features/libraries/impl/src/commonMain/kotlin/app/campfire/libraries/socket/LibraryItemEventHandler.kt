// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.socket

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.network.models.LibraryItemExpanded
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Handles library-item-scoped socket events by writing through to the local DB.
 * The `LibraryItemSourceOfTruth` observes the same DB rows, so all downstream
 * consumers (detail screens, paged lists) receive the updates automatically.
 */
interface LibraryItemEventHandler {
  suspend fun onItemAdded(item: LibraryItemExpanded)
  suspend fun onItemUpdated(item: LibraryItemExpanded)
  suspend fun onItemRemoved(itemId: LibraryItemId)
  suspend fun onItemsAdded(items: List<LibraryItemExpanded>)
  suspend fun onItemsUpdated(items: List<LibraryItemExpanded>)
}

@ContributesBinding(UserScope::class)
@Inject
class DefaultLibraryItemEventHandler(
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val dispatcherProvider: DispatcherProvider,
) : LibraryItemEventHandler {

  override suspend fun onItemAdded(item: LibraryItemExpanded) {
    libraryItemDao.insert(item, asTransaction = true)
  }

  override suspend fun onItemUpdated(item: LibraryItemExpanded) {
    libraryItemDao.insert(item, asTransaction = true)
  }

  override suspend fun onItemRemoved(itemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.libraryItemsQueries.deleteForId(itemId)
    }
  }

  override suspend fun onItemsAdded(items: List<LibraryItemExpanded>) {
    items.forEach { libraryItemDao.insert(it, asTransaction = true) }
  }

  override suspend fun onItemsUpdated(items: List<LibraryItemExpanded>) {
    items.forEach { libraryItemDao.insert(it, asTransaction = true) }
  }
}
