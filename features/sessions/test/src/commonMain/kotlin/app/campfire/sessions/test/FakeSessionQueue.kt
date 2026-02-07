package app.campfire.sessions.test

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.api.SessionQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

class FakeSessionQueue : SessionQueue {
  val queue = ArrayDeque<LibraryItem>()
  private val queueFlow = MutableSharedFlow<List<LibraryItem>>()

  override suspend fun add(libraryItem: LibraryItem) {
    queue.addLast(libraryItem)
    emit()
  }

  override suspend fun remove(libraryItem: LibraryItem) {
    queue.remove(libraryItem)
    emit()
  }

  override suspend fun pop(): LibraryItem? {
    return queue.removeFirstOrNull().also {
      emit()
    }
  }

  override suspend fun reorder(
    fromItemId: LibraryItemId,
    toItemId: LibraryItemId,
  ) {
    val fromIndex = queue.indexOfFirst { it.id == fromItemId }
    val toIndex = queue.indexOfFirst { it.id == toItemId }
    queue.add(toIndex, queue.removeAt(fromIndex))
    emit()
  }

  override suspend fun clear() {
    queue.clear()
    emit()
  }

  override fun observeAll(): Flow<List<LibraryItem>> {
    return queueFlow
      .onSubscription {
        emit(queue)
      }
  }

  private suspend fun emit() {
    queueFlow.emit(queue)
  }
}
