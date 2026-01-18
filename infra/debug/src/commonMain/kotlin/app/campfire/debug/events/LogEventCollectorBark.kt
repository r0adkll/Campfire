package app.campfire.debug.events

import app.campfire.core.logging.Extras
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogPriority
import app.campfire.core.time.FatherTime
import app.campfire.debug.events.storage.EventStorage
import me.tatarka.inject.annotations.Inject

@Inject
class LogEventCollectorBark(
  private val storage: EventStorage,
  private val fatherTime: FatherTime,
) : Heartwood.Bark {

  override fun log(priority: LogPriority, tag: String?, extras: Extras?, message: () -> String) {
    // Do nothing
  }

  override fun log(priority: LogPriority, tag: String?, extras: Extras?, message: () -> String, throwable: Throwable?) {
    val event = LogEvent(
      priority = priority,
      tag = tag,
      message = message(),
      throwable = throwable,
      extras = extras,
      timestamp = fatherTime.nowInEpochMillis(),
    )

    storage.put(event)
  }
}
