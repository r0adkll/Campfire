package app.campfire.audioplayer.test.history

import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart

class FakePlaybackHistoryRepository : PlaybackHistoryRepository {

  val history = mutableMapOf<LibraryItemId, List<PlaybackAction>>()

  val updatesFlow = MutableSharedFlow<Unit>()

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observe(libraryItemId: LibraryItemId): Flow<List<PlaybackAction>> {
    return updatesFlow
      .mapLatest {
        history[libraryItemId] ?: emptyList()
      }
      .onStart {
        history[libraryItemId]?.let {
          emit(it)
        }
      }
  }

  override suspend fun get(libraryItemId: LibraryItemId): List<PlaybackAction> {
    return history[libraryItemId] ?: emptyList()
  }

  override suspend fun clear(libraryItemId: LibraryItemId) {
    history[libraryItemId] = emptyList()
    updatesFlow.emit(Unit)
  }

  override suspend fun clearAll() {
    history.clear()
    updatesFlow.emit(Unit)
  }
}
