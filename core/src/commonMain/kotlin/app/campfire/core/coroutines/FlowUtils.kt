package app.campfire.core.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T, R> Flow<T?>.mapIfNotNull(mapper: suspend (T) -> R): Flow<R?> = map {
  if (it != null) mapper(it) else null
}
