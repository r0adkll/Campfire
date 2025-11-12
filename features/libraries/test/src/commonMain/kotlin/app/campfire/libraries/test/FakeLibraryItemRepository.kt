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
}
