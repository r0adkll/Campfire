package app.campfire.home.api

import app.campfire.home.api.model.Shelf
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

  /**
   * Observe a flow of the users personalized home feed
   */
  fun observeHomeFeed(): Flow<List<Shelf<*>>>
}
