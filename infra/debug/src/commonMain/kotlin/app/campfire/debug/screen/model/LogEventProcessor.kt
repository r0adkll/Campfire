package app.campfire.debug.screen.model

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.debug.events.LogEvent
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@Inject
class LogEventProcessor(
  private val dispatcherProvider: DispatcherProvider,
) {

  suspend fun process(event: LogEvent): EventUiModel = withContext(dispatcherProvider.computation) {
    val eventType = EventType.entries.find {
      event.message.startsWith(it.key)
    } ?: EventType.None

    EventUiModel(
      type = eventType,
      priority = event.priority,
      message = event.message,
      throwable = event.throwable,
      tags = buildList {
        event.tag?.let { add(it) }
      }.toPersistentList(),
      timestamp = event.timestamp,
    )
  }
}
