package app.campfire.debug.events.storage

import app.campfire.debug.events.LogEvent
import kotlinx.coroutines.flow.SharedFlow

interface EventStorage {

  fun observeAll(): SharedFlow<List<LogEvent>>
  fun put(event: LogEvent)
}
