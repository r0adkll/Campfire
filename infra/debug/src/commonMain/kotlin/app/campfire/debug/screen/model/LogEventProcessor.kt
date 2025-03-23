package app.campfire.debug.screen.model

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.LogPriority
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

    var priority = event.priority
    var message = event.message
    val tags = buildList<String> {
//      event.tag?.let { add(it) }
    }.toMutableList()

    // Parse network specific information out of certain logs
    if (eventType == EventType.NetworkRequest || eventType == EventType.NetworkResponse) {
      var url: String? = null
      var method: String? = null
      var resultCode: Int? = null

      event.message.lineSequence().forEach { line ->
        RequestRegex.find(line)?.let { requestMatch ->
          requestMatch.groupValues.getOrNull(1)?.let { url = it }
        }

        ResponseRegex.find(line)?.let { responseMatch ->
          responseMatch.groupValues.getOrNull(1)?.let { resultCode = it.trim().toIntOrNull() }
        }

        MethodRegex.find(line)?.let { methodMatch ->
          methodMatch.groupValues.getOrNull(1)?.let { method = it }
        }

        FromRegex.find(line)?.let { findMatch ->
          findMatch.groupValues.getOrNull(1)?.let { url = it }
        }
      }

      if (method != null && url != null) {
        message = buildString {
          if (eventType == EventType.NetworkRequest) {
            append("-->")
          } else if (resultCode in 200..299) {
            append("<--")
          } else {
            append("<!--")
          }
          append(" $url")
        }

        tags.add(method!!)

        if (resultCode != null && resultCode != 200) {
          tags.add(resultCode.toString())
        }
      }

      when (resultCode) {
        in 200..299 -> priority = LogPriority.INFO
        in 400..499 -> priority = LogPriority.WARN
        in 500..599 -> priority = LogPriority.ERROR
      }
    }

    EventUiModel(
      type = eventType,
      priority = priority,
      message = message,
      throwable = event.throwable,
      tags = tags.toPersistentList(),
      timestamp = event.timestamp,
    )
  }
}

internal val RequestRegex = "REQUEST: (.*)".toRegex()
internal val ResponseRegex = "RESPONSE: (.*)".toRegex()
internal val MethodRegex = "METHOD: (.*)".toRegex()
internal val FromRegex = "FROM: (.*)".toRegex()
