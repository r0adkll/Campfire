package app.campfire.debug.events

import app.campfire.core.logging.Extras
import app.campfire.core.logging.LogPriority

data class LogEvent(
  val priority: LogPriority,
  val tag: String?,
  val message: String,
  val throwable: Throwable?,
  val extras: Extras?,
  val timestamp: Long,
)
