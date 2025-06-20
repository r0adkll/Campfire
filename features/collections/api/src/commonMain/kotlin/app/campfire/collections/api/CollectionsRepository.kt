package app.campfire.collections.api

import app.campfire.core.model.Collection
import app.campfire.core.model.CollectionId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

interface CollectionsRepository {

  /**
   * Observe the list of [Collection] for the current library
   * @return a flow of all [Collection]s
   */
  fun observeAllCollections(): Flow<List<Collection>>

  /**
   * Observe a single collection for a given item
   * @param collectionId the id of the collection to observe
   * @return a flow of the [Collection]
   */
  fun observeCollection(collectionId: CollectionId): Flow<Collection>

  /**
   * Observe the list of [LibraryItem] for a given [Collection]
   */
  fun observeCollectionItems(collectionId: CollectionId): Flow<List<LibraryItem>>

  /**
   * Create a new collection with a given [name] and [description]
   * @param name the name of the collection
   * @param bookIds the ids of the books to add to the collection. Must contain at least 1!
   * @param description the description of the collection
   * @return the id of the newly created [Collection]
   */
  suspend fun createCollection(
    name: String,
    bookIds: List<String>,
    description: String? = null,
  ): Result<CollectionId>

  /**
   * Update a collection with a new name and/or description
   * @param collectionId the id of the collection to update
   * @param name the new name of the collection, or null to not update
   * @param description the new description of the collection, or null to not update
   * @return the result of the update
   */
  suspend fun updateCollection(
    collectionId: CollectionId,
    name: String? = null,
    description: String? = null,
  ): Result<Unit>

  /**
   * Delete a collection
   * @param collectionId the id of the collection to delete
   * @return the result of the delete
   */
  suspend fun deleteCollection(
    collectionId: CollectionId,
  ): Result<Unit>

  /**
   * Add a book to a collection
   * @param bookId the id of the book to add
   * @param collectionId the id of the collection to add the book to
   * @return the result of the add
   */
  suspend fun addToCollection(
    bookId: LibraryItemId,
    collectionId: CollectionId,
  ): Result<Unit>

  /**
   * Remove a book from a collection
   * @param bookId the id of the book to remove
   * @param collectionId the id of the collection to remove the book from
   * @return the result of the remove
   */
  suspend fun removeFromCollection(
    bookIds: List<LibraryItemId>,
    collectionId: CollectionId,
  ): Result<Unit>
}
