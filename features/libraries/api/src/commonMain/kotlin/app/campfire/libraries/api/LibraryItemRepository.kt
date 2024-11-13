package app.campfire.libraries.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

interface LibraryItemRepository {

  fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem>

  suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem
}
