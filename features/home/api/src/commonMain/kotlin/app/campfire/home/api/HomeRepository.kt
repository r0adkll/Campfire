package app.campfire.home.api

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.home.api.model.Shelf
import app.campfire.home.api.model.ShelfId
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

  /**
   * Observe a flow of the users personalized home feed
   */
  fun observeHomeFeed(): Flow<FeedResponse<List<Shelf>>>

  /**
   * Observe the [MediaProgress] for each libraryItemId passed to the function
   */
  fun observeMediaProgress(libraryItemIds: List<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>>

  /**
   * Observe the contents of a single [Shelf] by its id.
   */
  fun observeShelf(shelfId: ShelfId, shelfType: ShelfType): Flow<List<ShelfEntity>>
}
