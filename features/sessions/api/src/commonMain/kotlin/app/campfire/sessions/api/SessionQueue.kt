package app.campfire.sessions.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

interface SessionQueue {

  suspend fun add(libraryItem: LibraryItem)
  suspend fun remove(libraryItem: LibraryItem)
  suspend fun pop(): LibraryItem?
  suspend fun reorder(fromItemId: LibraryItemId, toItemId: LibraryItemId)
  suspend fun clear()

  fun observeAll(): Flow<List<LibraryItem>>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun SessionQueue.observeContains(libraryItemId: LibraryItemId): Flow<Boolean> {
  return observeAll()
    .mapLatest { it.any { item -> item.id == libraryItemId } }
}
