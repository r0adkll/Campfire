package app.campfire.collections.api

import app.campfire.core.model.Collection
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryItem
import kotlinx.coroutines.flow.Flow

interface CollectionsRepository {

  /**
   * Observe the list of [Collection] for the current library
   */
  fun observeAllCollections(): Flow<List<Collection>>

  /**
   * Observe the list of [LibraryItem] for a given [Collection]
   */
  fun observeCollectionItems(collectionId: CollectionId): Flow<List<LibraryItem>>
}
