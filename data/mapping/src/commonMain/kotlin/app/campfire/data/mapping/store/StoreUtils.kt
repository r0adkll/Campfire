package app.campfire.data.mapping.store

import app.campfire.core.logging.bark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Helper function for debugging streams from a Store.
 */
@Suppress("UnusedFlow")
fun <T> Flow<StoreReadResponse<T>>.debugLogging(tag: String, enabled: Boolean = true): Flow<StoreReadResponse<T>> {
  if (!enabled) return this
  return onEach { response ->
    when (response) {
      is StoreReadResponse.Error.Exception -> bark(tag, throwable = response.error) { "Store[$tag] Error" }
      is StoreReadResponse.Error.Message -> bark(tag) { "Store[$tag] Error: ${response.message}" }
      is StoreReadResponse.Loading -> bark(tag) { "Store[$tag] Loading" }
      is StoreReadResponse.NoNewData -> bark(tag) { "Store[$tag] NoNewData" }
      is StoreReadResponse.Data<*> -> bark(tag) { "Store[$tag Data" }
    }
  }
}
