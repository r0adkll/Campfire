package app.campfire.android.plugin.analytics

import app.campfire.analytics.Analytics
import app.campfire.analytics.events.AnalyticEvent
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * An [Analytics] sink that records every event flowing through the app's analytics
 * pipeline for the Analytics Livewire plugin. Registered into [Analytics.Delegator]
 * by LivewireInitializer, alongside the real destinations.
 */
object LivewireAnalytics : Analytics {

  data class RecordedEvent(
    val id: Long,
    val timeMs: Long,
    val name: String,
    val params: Map<String, String>,
  )

  override val debugState: String
    get() = "LivewireAnalytics(recorded=${_events.value.size})"

  private val idGenerator = AtomicLong(0L)

  private val _events = MutableStateFlow<List<RecordedEvent>>(emptyList())
  val events: StateFlow<List<RecordedEvent>> = _events.asStateFlow()

  override fun send(event: AnalyticEvent) {
    val recorded = RecordedEvent(
      id = idGenerator.incrementAndGet(),
      timeMs = System.currentTimeMillis(),
      name = event.eventName,
      params = event.params?.mapValues { (_, value) -> value.toString() } ?: emptyMap(),
    )
    _events.update { current -> (listOf(recorded) + current).take(MAX_EVENTS) }
  }

  fun clear() {
    _events.value = emptyList()
  }

  private const val MAX_EVENTS = 500
}
