package app.campfire.data.mapping.store

import app.campfire.core.logging.LogPriority.ERROR
import app.campfire.core.logging.LogPriority.VERBOSE
import app.campfire.core.logging.bark
import app.campfire.crashreporting.CrashReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Helper function for debugging streams from a Store.
 */
@Suppress("UnusedFlow")
fun <T> Flow<StoreReadResponse<T>>.debugLogging(
  tag: String,
  enabled: Boolean = true,
): Flow<StoreReadResponse<T>> {
  if (!enabled) return this
  return onEach { response ->
    when (response) {
      is StoreReadResponse.Error.Exception -> {
        bark(tag, ERROR, throwable = response.error) { "Store[$tag] Error" }
        CrashReporter.record(
          StoreException("$tag - Error observing store instance", response.error),
        )
      }
      is StoreReadResponse.Error.Message -> {
        bark(tag, ERROR) { "Store[$tag] Error: ${response.message}" }
        CrashReporter.record(
          StoreException("$tag - ${response.message}"),
        )
      }
      is StoreReadResponse.Loading -> bark(tag, VERBOSE) { "Store[$tag] Loading" }
      is StoreReadResponse.NoNewData -> bark(tag, VERBOSE) { "Store[$tag] NoNewData" }
      is StoreReadResponse.Data<*> -> bark(tag, VERBOSE) { "Store[$tag] Data" }
    }
  }
}

class StoreException(message: String?, cause: Throwable? = null) : Exception(message, cause)
