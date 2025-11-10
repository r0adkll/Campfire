package app.campfire.home.ui

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.home.api.FeedResponse
import app.campfire.home.api.HomeRepository
import app.campfire.home.api.model.Shelf
import app.campfire.home.api.model.ShelfId
import kotlinx.coroutines.flow.Flow

class FakeHomeRepository(
  private val homeFeedFlowFactory: () -> Flow<FeedResponse<List<Shelf>>>,
  private val mediaProgressFlowFactory: (Set<LibraryItemId>) -> Flow<Map<LibraryItemId, MediaProgress>>,
  private val shelfEntityFlowFactory: (ShelfId, ShelfType) -> Flow<List<ShelfEntity>>,
) : HomeRepository {

  override fun observeHomeFeed(): Flow<FeedResponse<List<Shelf>>> {
    return homeFeedFlowFactory()
  }

  override fun observeMediaProgress(libraryItemIds: Set<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>> {
    return mediaProgressFlowFactory(libraryItemIds)
  }

  override fun observeShelf(
    shelfId: ShelfId,
    shelfType: ShelfType,
  ): Flow<List<ShelfEntity>> {
    return shelfEntityFlowFactory(shelfId, shelfType)
  }
}
