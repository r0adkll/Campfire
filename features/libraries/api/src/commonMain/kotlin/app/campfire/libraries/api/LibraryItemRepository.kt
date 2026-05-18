package app.campfire.libraries.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

interface LibraryItemRepository {

  fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem>

  suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem

  /**
   * Delete a library item from the server. When [hardDelete] is true the
   * server also removes the item's files from disk; otherwise only the DB
   * row + cascading server-side data is removed.
   *
   * On success the local cache for this item is cleared. Cleanup of any
   * locally-downloaded audio files is the caller's responsibility.
   */
  suspend fun deleteLibraryItem(itemId: LibraryItemId, hardDelete: Boolean): Result<Unit>
}
