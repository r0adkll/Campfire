package app.campfire.debug.events.storage

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.debug.events.LogEvent
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class InMemoryEventStorage(
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : EventStorage {

  private val eventChannel = Channel<LogEvent>(
    capacity = 20,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  private val eventFlow = MutableSharedFlow<List<LogEvent>>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  private val events = ArrayDeque<LogEvent>()

  init {
    applicationScope.launch {
      while (isActive) {
        val event = eventChannel.receive()
        events.addFirst(event)
        if (events.size > MAX_EVENTS) {
          events.removeLast()
        }

        eventFlow.emit(events.toPersistentList())
      }
    }
  }

  override fun observeAll(): SharedFlow<List<LogEvent>> {
    return eventFlow.asSharedFlow()
  }

  override fun put(event: LogEvent) {
    val result = eventChannel.trySend(event)
    if (result.isFailure) {
      println("Failed to send event - Reason: ${result.exceptionOrNull()?.message}")
    }
  }

  companion object {
    private const val MAX_EVENTS = 500
  }
}
