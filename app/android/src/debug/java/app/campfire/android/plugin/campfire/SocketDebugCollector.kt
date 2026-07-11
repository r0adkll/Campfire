package app.campfire.android.plugin.campfire

import app.campfire.socket.SocketManager
import app.campfire.socket.SocketState
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Records the socket connection state transitions and every [app.campfire.socket.events.SocketEvent]
 * for the Campfire plugin's Socket tab. Attached once at app init (LivewireInitializer) so
 * history accumulates even while the plugin is closed.
 */
object SocketDebugCollector {

  data class StateTransition(
    val timeMs: Long,
    val state: SocketState,
  )

  data class RecordedEvent(
    val id: Long,
    val timeMs: Long,
    val name: String,
    val details: String,
  )

  private val idGenerator = AtomicLong(0L)

  private val _stateHistory = MutableStateFlow<List<StateTransition>>(emptyList())
  val stateHistory: StateFlow<List<StateTransition>> = _stateHistory.asStateFlow()

  private val _events = MutableStateFlow<List<RecordedEvent>>(emptyList())
  val events: StateFlow<List<RecordedEvent>> = _events.asStateFlow()

  private var attached = false

  fun attach(socketManager: SocketManager, scope: CoroutineScope) {
    if (attached) return
    attached = true

    scope.launch {
      socketManager.state.collect { state ->
        _stateHistory.update { current ->
          (listOf(StateTransition(System.currentTimeMillis(), state)) + current).take(MAX_ENTRIES)
        }
      }
    }
    scope.launch {
      socketManager.events.collect { event ->
        val recorded = RecordedEvent(
          id = idGenerator.incrementAndGet(),
          timeMs = System.currentTimeMillis(),
          name = event::class.simpleName ?: "SocketEvent",
          details = event.toString(),
        )
        _events.update { current -> (listOf(recorded) + current).take(MAX_ENTRIES) }
      }
    }
  }

  fun clearEvents() {
    _events.value = emptyList()
  }

  private const val MAX_ENTRIES = 200
}
