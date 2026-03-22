package app.campfire.audioplayer.test.history

import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import kotlin.time.Duration
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDateTime

class FakePlaybackHistoryRepository : PlaybackHistoryRepository {

  private val idGenerator = atomic(0L)
  val history = mutableMapOf<LibraryItemId, List<PlaybackAction>>()

  private val updatesFlow = MutableSharedFlow<Unit>()

  override suspend fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration?,
  ) {
    val itemHistory = history[libraryItemId]?.toMutableList() ?: mutableListOf()
    itemHistory += PlaybackAction(
      id = idGenerator.getAndIncrement(),
      libraryItemId = libraryItemId,
      userId = "test_user",
      type = type,
      fromPosition = fromPosition,
      toPosition = toPosition,
      timestamp = LocalDateTime(2026, 5, 1, 1, 0),
    )
    updatesFlow.emit(Unit)
  }

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
