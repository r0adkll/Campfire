// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

interface LibraryItemRepository {

  fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem>

  suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem

  /**
   * The item as the server has it now, which is cached too. A cached copy can be one a list
   * fetched, which has a book's authors, narrators and series only as names to show.
   *
   * @return a failure when the server can't be reached or no longer has the item
   */
  suspend fun fetchLibraryItem(itemId: LibraryItemId): Result<LibraryItem>

  /**
   * Delete a library item from the server. When [hardDelete] is true the
   * server also removes the item's files from disk; otherwise only the DB
   * row + cascading server-side data is removed.
   *
   * On success the item is purged here too, from every list it was in and with its
   * downloads, as [LibraryItemPurger] does for an item the server reports removed.
   *
   * @return a failure, an [ItemChangeNotAllowedException] when the account may not delete items
   */
  suspend fun deleteLibraryItem(itemId: LibraryItemId, hardDelete: Boolean): Result<Unit>

  /**
   * Have the server scan the item's files again, picking up files added, removed or changed on
   * disk since its last scan. An updated item is fetched again here.
   *
   * @return a failure, an [ItemChangeNotAllowedException] when the account may not rescan items
   */
  suspend fun rescanLibraryItem(itemId: LibraryItemId): Result<RescanResult>

  /**
   * Change a book's details on the server, only those [update] sets. A changed item is fetched
   * again here.
   *
   * @return whether the server changed anything, or a failure, an [ItemChangeNotAllowedException]
   * when the account may not change items
   */
  suspend fun updateBookDetails(itemId: LibraryItemId, update: BookDetailsUpdate): Result<Boolean>
}
