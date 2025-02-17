package app.campfire.debug.screen.model

import app.campfire.core.logging.LogPriority
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class EventUiModel(
  val type: EventType,
  val priority: LogPriority,
  val message: String,
  val throwable: Throwable? = null,
  val tags: ImmutableList<String> = persistentListOf(),
  val timestamp: Long,
)

enum class EventType(val key: String) {
  Send("-->"),
  TrySend("~~>"),
  Receive("<--"),
  ReceiveFailure("<!--"),
  None("1234567890"),
}
