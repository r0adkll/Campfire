package app.campfire.data.mapping

import org.mobilenativefoundation.store.store5.FetcherResult

fun <T : Any> Result<T>.asFetcherResult(): FetcherResult<T> {
  return when {
    isSuccess -> FetcherResult.Data(getOrThrow(), "api")
    else -> FetcherResult.Error.Exception(exceptionOrNull()!!)
  }
}
