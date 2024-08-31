package app.campfire.collections.api

import app.campfire.core.model.Collection
import kotlinx.coroutines.flow.Flow

interface CollectionsRepository {

  /**
   * Observe the list of [Collection] for the current library
   */
  fun observeAllCollections(): Flow<List<Collection>>
}
