package app.campfire.data.mapping.store

import app.campfire.core.logging.LogPriority.ERROR
import app.campfire.core.logging.LogPriority.VERBOSE
import app.campfire.core.logging.bark
import app.campfire.crashreporting.CrashReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin

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
    val messagePrefix = "Store[$tag] Origin[${response.origin.readable()}]"
    when (response) {
      is StoreReadResponse.Error.Exception -> {
        bark(tag, ERROR, throwable = response.error) { "$messagePrefix Error" }
        CrashReporter.record(
          StoreException("$tag - Error observing store instance", response.error),
        )
      }
      is StoreReadResponse.Error.Message -> {
        bark(tag, ERROR) { "$messagePrefix Error: ${response.message}" }
        CrashReporter.record(
          StoreException("$tag - ${response.message}"),
        )
      }
      is StoreReadResponse.Loading -> bark(tag, VERBOSE) { "$messagePrefix Loading" }
      is StoreReadResponse.NoNewData -> bark(tag, VERBOSE) { "$messagePrefix NoNewData" }
      is StoreReadResponse.Data<*> -> bark(tag, VERBOSE) { "$messagePrefix Data" }
    }
  }.onCompletion {
    bark(tag, VERBOSE) { "Store[$tag] onCompletion($it)" }
  }.onStart {
    bark(tag, VERBOSE) { "Store[$tag] onStart()" }
  }
}

private fun StoreReadResponseOrigin.readable(): String = when (this) {
  StoreReadResponseOrigin.Cache -> "Cache"
  StoreReadResponseOrigin.SourceOfTruth -> "SourceOfTruth"
  is StoreReadResponseOrigin.Fetcher -> "Fetcher(${this.name})"
}

class StoreException(message: String?, cause: Throwable? = null) : Exception(message, cause)
